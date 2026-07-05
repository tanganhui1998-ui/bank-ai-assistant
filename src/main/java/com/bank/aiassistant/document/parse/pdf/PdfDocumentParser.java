package com.bank.aiassistant.document.parse.pdf;

import com.bank.aiassistant.config.PdfParseProperties;
import com.bank.aiassistant.document.parse.DocumentChunkQualityService;
import com.bank.aiassistant.document.parse.DocumentParser;
import com.bank.aiassistant.document.parse.model.ChunkDraft;
import com.bank.aiassistant.document.parse.model.ParsedPdfDocument;
import com.bank.aiassistant.document.parse.word.WordStructureExtractor;
import com.bank.aiassistant.domain.entity.Document;
import com.bank.aiassistant.domain.entity.DocumentChunk;
import com.bank.aiassistant.domain.enums.ChunkStatus;
import com.bank.aiassistant.oss.OssService;
import com.bank.aiassistant.repository.DocumentChunkRepository;
import com.bank.aiassistant.search.BulkIndexResult;
import com.bank.aiassistant.search.KnowledgeChunkEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PDF 文档解析主实现。
 *
 * 主流程：
 * 1. 从 OSS 下载 PDF 文件流。
 * 2. 使用 PDFBox 提取逐页文本、字体和位置信息。
 * 3. 使用 Tabula 提取页面表格。
 * 4. 对文本进行清洗并过滤目录页。
 * 5. 按标题路径智能切片，表格独立切片。
 * 6. 将切片保存到 knowledge_chunk 表。
 *
 * 该类只负责“解析并保存切片”。文档状态流转仍由 DocumentParseWorkflowService
 * 统一控制，确保 RabbitMQ 重试逻辑和解析实现解耦。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(PdfParseProperties.class)
public class PdfDocumentParser implements DocumentParser {

    private final OssService ossService;
    private final PdfStructureExtractor structureExtractor;
    private final PdfChunker pdfChunker;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeChunkEsService knowledgeChunkEsService;
    private final PdfParseProperties pdfParseProperties;
    private final DocumentChunkQualityService chunkQualityService;
    private final WordStructureExtractor wordStructureExtractor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void parse(Document document) {
        String fileType = resolveFileType(document);
        if ("pdf".equals(fileType)) {
            parsePdf(document);
            return;
        }
        if ("doc".equals(fileType) || "docx".equals(fileType)) {
            parseWord(document);
            return;
        }
        throw new IllegalArgumentException("Unsupported document file type, documentId="
                + document.getDocumentId() + ", fileType=" + document.getFileType());
    }

    private void parsePdf(Document document) {
        log.info("Start PDF parse, documentId={}, objectKey={}", document.getDocumentId(), document.getOssObjectKey());

        try (InputStream inputStream = new BufferedInputStream(ossService.download(document.getOssObjectKey()));
             PDDocument pdfDocument = PDDocument.load(inputStream)) {

            ParsedPdfDocument parsedDocument = structureExtractor.extract(pdfDocument, document.getDocumentId());
            List<ChunkDraft> drafts = pdfChunker.chunk(parsedDocument);
            List<DocumentChunk> chunks = saveChunks(document, drafts);
            indexChunksToElasticsearch(document, chunks);

            log.info("PDF parse finished, documentId={}, pageCount={}, chunkCount={}",
                    document.getDocumentId(), pdfDocument.getNumberOfPages(), drafts.size());
        } catch (Exception ex) {
            log.error("PDF parse failed, documentId={}, objectKey={}",
                    document.getDocumentId(), document.getOssObjectKey(), ex);
            throw new IllegalStateException("PDF parse failed for document " + document.getDocumentId(), ex);
        }
    }

    private void parseWord(Document document) {
        log.info("Start Word parse, documentId={}, objectKey={}", document.getDocumentId(), document.getOssObjectKey());
        try (InputStream inputStream = new BufferedInputStream(ossService.download(document.getOssObjectKey()))) {
            ParsedPdfDocument parsedDocument = wordStructureExtractor.extract(
                    inputStream,
                    document.getFileName(),
                    resolveFileType(document));
            List<ChunkDraft> drafts = pdfChunker.chunk(parsedDocument);
            List<DocumentChunk> chunks = saveChunks(document, drafts);
            indexChunksToElasticsearch(document, chunks);
            log.info("Word parse finished, documentId={}, chunkCount={}", document.getDocumentId(), drafts.size());
        } catch (Exception ex) {
            log.error("Word parse failed, documentId={}, objectKey={}",
                    document.getDocumentId(), document.getOssObjectKey(), ex);
            throw new IllegalStateException("Word parse failed for document " + document.getDocumentId(), ex);
        }
    }

    private String resolveFileType(Document document) {
        String fileType = document.getFileType() == null ? "" : document.getFileType().toLowerCase(Locale.ROOT);
        String fileName = document.getFileName() == null ? "" : document.getFileName().toLowerCase(Locale.ROOT);
        if (!fileType.isBlank()) {
            return fileType.replace(".", "");
        }
        int index = fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index + 1) : "";
    }

    /**
     * 保存切片前先删除同文档旧切片。
     *
     * RabbitMQ 失败重试可能多次进入解析流程，先清理旧数据可以保证同一文档的
     * knowledge_chunk 结果始终来自最后一次成功解析。
     */
    private List<DocumentChunk> saveChunks(Document document, List<ChunkDraft> drafts) {
        chunkRepository.deleteByDocumentDocumentId(document.getDocumentId());
        List<DocumentChunk> chunks = new ArrayList<>();
        for (ChunkDraft draft : drafts) {
            chunks.add(DocumentChunk.builder()
                    .chunkId(document.getDocumentId() + "_" + draft.chunkSeq())
                    .documentId(document.getDocumentId())
                    .document(document)
                    .content(draft.content())
                    .chapterPath(draft.chapterPath())
                    .chapterNo(draft.chapterNo())
                    .chunkSeq(draft.chunkSeq())
                    .startPage(draft.startPage())
                    .endPage(draft.endPage())
                    .tokenCount(draft.tokenCount())
                    .qualityScore(chunkQualityService.score(draft.content()))
                    .status(resolveChunkStatus(draft.content()))
                .build());
        }
        chunkRepository.saveAll(chunks);
        log.info("Saved PDF chunks, documentId={}, chunkCount={}", document.getDocumentId(), chunks.size());
        return chunks;
    }

    /**
     * 将数据库切片同步写入 Elasticsearch。
     *
     * 这里在解析器内部执行 ES 写入，是为了保证 DocumentParseWorkflowService
     * 只有在 ES 写入成功后才会把文档状态更新为 PARSE_COMPLETED。
     */
    private void indexChunksToElasticsearch(Document document, List<DocumentChunk> chunks) {
        knowledgeChunkEsService.deleteByDocumentId(document.getDocumentId());
        List<DocumentChunk> validChunks = chunks.stream()
                .filter(chunk -> chunk.getStatus() == ChunkStatus.VALID)
                .toList();
        BulkIndexResult result = knowledgeChunkEsService.indexChunks(validChunks);
        if (result.hasFailure()) {
            throw new IllegalStateException("ES chunk index partially failed, documentId="
                    + document.getDocumentId() + ", failureCount=" + result.failureCount());
        }
    }

    private ChunkStatus resolveChunkStatus(String content) {
        double score = chunkQualityService.score(content);
        return score >= pdfParseProperties.getMinQualityScore() ? ChunkStatus.VALID : ChunkStatus.EXPIRED;
    }
}
