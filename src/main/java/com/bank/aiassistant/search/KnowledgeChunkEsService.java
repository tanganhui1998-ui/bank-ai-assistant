package com.bank.aiassistant.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.bank.aiassistant.config.KnowledgeElasticsearchProperties;
import com.bank.aiassistant.domain.entity.Document;
import com.bank.aiassistant.domain.entity.DocumentChunk;
import com.bank.aiassistant.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库切片 ES 入库服务。
 *
 * 负责：
 * - 批量调用 Embedding 服务生成 1536 维向量。
 * - 使用 Elasticsearch BulkRequest 批量写入切片。
 * - 记录单条失败，不影响同批其他切片写入。
 * - 按文档 ID 删除 ES 中的旧切片，支持文档版本更新和重新解析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeChunkEsService {

    private final ElasticsearchClient elasticsearchClient;
    private final RestClient restClient;
    private final KnowledgeElasticsearchProperties properties;
    private final EmbeddingService embeddingService;

    public BulkIndexResult indexChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new BulkIndexResult(0, 0, 0);
        }

        List<String> texts = chunks.stream()
                .map(this::buildEmbeddingInput)
                .toList();
        List<List<Float>> embeddings = embeddingService.embedBatch(texts);

        List<KnowledgeChunkEsDocument> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            documents.add(toEsDocument(chunks.get(i), embeddings.get(i)));
        }

        BulkResponse response = executeBulk(documents);
        int failureCount = 0;
        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                failureCount++;
                log.error("ES bulk index item failed, chunkId={}, reason={}",
                        item.id(), item.error().reason());
            }
        }
        int successCount = documents.size() - failureCount;
        log.info("ES bulk index finished, totalCount={}, successCount={}, failureCount={}",
                documents.size(), successCount, failureCount);
        return new BulkIndexResult(documents.size(), successCount, failureCount);
    }

    public void deleteByDocumentId(String documentId) {
        try {
            Query query = Query.of(q -> q.term(t -> t
                    .field("documentId")
                    .value(documentId)));
            elasticsearchClient.deleteByQuery(request -> request
                    .index(properties.getChunkIndexName())
                    .query(query)
                    .refresh(true));
            log.info("Deleted ES chunks by documentId, documentId={}", documentId);
        } catch (IOException ex) {
            log.error("Failed to delete ES chunks by documentId, documentId={}", documentId, ex);
            throw new IllegalStateException("Failed to delete ES chunks, documentId=" + documentId, ex);
        }
    }

    public void markDocumentExpired(String documentId) {
        updateDocumentVersionFields(documentId, "EXPIRED", false);
    }

    public void markDocumentPublished(String documentId) {
        updateDocumentVersionFields(documentId, "PUBLISHED", true);
    }

    /**
     * 更新 ES 中同一文档全部切片的版本状态。
     *
     * 使用 update_by_query 而不是重新写入向量，避免发布/废弃动作重复调用 Embedding。
     */
    private void updateDocumentVersionFields(String documentId, String status, boolean latestVersion) {
        try {
            Request request = new Request("POST", "/" + properties.getChunkIndexName() + "/_update_by_query");
            request.addParameter("refresh", "true");
            request.setJsonEntity("""
                    {
                      "script": {
                        "source": "ctx._source.status = params.status; ctx._source.latestVersion = params.latestVersion",
                        "lang": "painless",
                        "params": {
                          "status": "%s",
                          "latestVersion": %s
                        }
                      },
                      "query": {
                        "term": {
                          "documentId": "%s"
                        }
                      }
                    }
                    """.formatted(status, latestVersion, documentId));
            restClient.performRequest(request);
            log.info("Updated ES document version fields, documentId={}, status={}, latestVersion={}",
                    documentId, status, latestVersion);
        } catch (IOException ex) {
            log.error("Failed to update ES document version fields, documentId={}", documentId, ex);
            throw new IllegalStateException("Failed to update ES document version fields, documentId=" + documentId, ex);
        }
    }

    private BulkResponse executeBulk(List<KnowledgeChunkEsDocument> documents) {
        BulkRequest.Builder builder = new BulkRequest.Builder()
                .index(properties.getChunkIndexName())
                .refresh(Refresh.True);

        for (KnowledgeChunkEsDocument document : documents) {
            builder.operations(operation -> operation.index(index -> index
                    .id(document.chunkId())
                    .document(document)));
        }

        try {
            return elasticsearchClient.bulk(builder.build());
        } catch (IOException ex) {
            log.error("ES bulk index request failed, indexName={}, documentCount={}",
                    properties.getChunkIndexName(), documents.size(), ex);
            throw new IllegalStateException("ES bulk index request failed", ex);
        }
    }

    /**
     * 向量化输入包含标题路径和正文，使 embedding 同时感知章节语义和正文内容。
     */
    private String buildEmbeddingInput(DocumentChunk chunk) {
        String chapterPath = chunk.getChapterPath() == null ? "" : chunk.getChapterPath();
        return chapterPath + "\n" + chunk.getContent();
    }

    private KnowledgeChunkEsDocument toEsDocument(DocumentChunk chunk, List<Float> embedding) {
        Document document = chunk.getDocument();
        return KnowledgeChunkEsDocument.builder()
                .chunkId(chunk.getChunkId())
                .documentId(document.getDocumentId())
                .documentName(document.getDisplayName())
                .documentType(document.getDocumentType() == null ? null : document.getDocumentType().name())
                .versionNo(document.getVersionNo())
                .department(document.getDepartment())
                .confidentialityLevel(document.getConfidentialityLevel() == null ? null : document.getConfidentialityLevel().name())
                .status(document.getProcessStatus() == null ? null : document.getProcessStatus().name())
                .chunkStatus(chunk.getStatus() == null ? null : chunk.getStatus().name())
                .latestVersion(false)
                .content(chunk.getContent())
                .embedding(embedding)
                .chapterPath(chunk.getChapterPath())
                .chapterNo(chunk.getChapterNo())
                .chunkSeq(chunk.getChunkSeq())
                .startPage(chunk.getStartPage())
                .endPage(chunk.getEndPage())
                .qualityScore(chunk.getQualityScore())
                .effectiveTime(document.getEffectiveTime())
                .publishedTime(document.getPublishedTime())
                .createdTime(LocalDateTime.now())
                .build();
    }
}
