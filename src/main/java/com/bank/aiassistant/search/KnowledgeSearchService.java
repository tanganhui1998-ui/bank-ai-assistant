package com.bank.aiassistant.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.bank.aiassistant.config.KnowledgeElasticsearchProperties;
import com.bank.aiassistant.security.PermissionFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 知识库检索服务。
 *
 * 当前实现提供基于 IK 分词的全文检索，并在 ES 查询层自动追加权限过滤。
 * 后续可以在同一过滤条件下扩展向量召回或混合检索。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final KnowledgeElasticsearchProperties properties;
    private final PermissionFilterService permissionFilterService;

    public List<KnowledgeSearchResult> search(KnowledgeSearchRequest request) {
        try {
            Query query = buildQuery(request.getQuery());
            return elasticsearchClient.search(search -> search
                            .index(properties.getChunkIndexName())
                            .size(Math.min(Math.max(request.getSize(), 1), 50))
                            .query(query),
                    KnowledgeChunkEsDocument.class
            ).hits().hits().stream()
                    .map(hit -> toResult(hit.source(), hit.score()))
                    .toList();
        } catch (IOException ex) {
            log.error("Knowledge search failed, query={}", request.getQuery(), ex);
            throw new IllegalStateException("Knowledge search failed", ex);
        }
    }

    private Query buildQuery(String keyword) {
        List<Query> filters = permissionFilterService.buildEsFilterQueries();
        return Query.of(q -> q.bool(bool -> {
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(keyword)
                    .fields("content^1.0", "chapterPath^2.0")));
            filters.forEach(bool::filter);
            return bool;
        }));
    }

    private KnowledgeSearchResult toResult(KnowledgeChunkEsDocument document, Double score) {
        return KnowledgeSearchResult.builder()
                .chunkId(document.chunkId())
                .documentId(document.documentId())
                .documentName(document.documentName())
                .versionNo(document.versionNo())
                .department(document.department())
                .confidentialityLevel(document.confidentialityLevel())
                .chapterPath(document.chapterPath())
                .content(document.content())
                .score(score)
                .build();
    }
}
