package com.bank.aiassistant.document.service;

import com.bank.aiassistant.config.DocumentTaskProperties;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.bank.aiassistant.document.dto.DocumentChunkPreviewResponse;
import com.bank.aiassistant.document.dto.DocumentResponse;
import com.bank.aiassistant.document.message.DocumentParseMessage;
import com.bank.aiassistant.document.message.DocumentParseMessageProducer;
import com.bank.aiassistant.domain.entity.Document;
import com.bank.aiassistant.domain.entity.DocumentChunk;
import com.bank.aiassistant.domain.enums.ChunkStatus;
import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import com.bank.aiassistant.exception.BusinessException;
import com.bank.aiassistant.repository.DocumentChunkRepository;
import com.bank.aiassistant.repository.DocumentRepository;
import com.bank.aiassistant.search.BulkIndexResult;
import com.bank.aiassistant.search.KnowledgeChunkEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库生产运维服务。
 *
 * 提供解析补偿、ES 重建和切片预览能力，方便管理员处理解析失败、索引漂移和切片质量排查。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeMaintenanceService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeChunkEsService knowledgeChunkEsService;
    private final DocumentParseMessageProducer parseMessageProducer;
    private final DocumentTaskProperties documentTaskProperties;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public DocumentResponse retryParse(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在"));
        if (document.getProcessStatus() == DocumentProcessStatus.PUBLISHED) {
            throw new BusinessException("DOCUMENT_ALREADY_PUBLISHED", "已发布文档不能直接重新解析，请先废弃或上传新版本");
        }
        document.setProcessStatus(DocumentProcessStatus.UPLOAD_COMPLETED);
        document.setParseErrorMessage(null);
        document.setRetryCount(0);
        documentRepository.save(document);

        CurrentUser user = currentUserProvider.currentUser();
        DocumentParseMessage message = new DocumentParseMessage(
                documentId,
                user.tenantId(),
                user.userId(),
                user.userName(),
                LocalDateTime.now(),
                0,
                documentTaskProperties.getParseMaxRetryCount());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                parseMessageProducer.send(message);
            }
        });
        log.info("Document parse retry message registered after transaction commit, documentId={}, operatorId={}",
                documentId, user.userId());
        return DocumentResponse.from(document);
    }

    public BulkIndexResult rebuildDocumentIndex(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在"));
        List<DocumentChunk> chunks = chunkRepository
                .findByDocumentDocumentIdAndStatusOrderByChunkSeqAsc(documentId, ChunkStatus.VALID);
        for (DocumentChunk chunk : chunks) {
            chunk.setDocument(document);
        }
        knowledgeChunkEsService.deleteByDocumentId(documentId);
        BulkIndexResult result = knowledgeChunkEsService.indexChunks(chunks);
        log.info("Rebuilt ES index for document, documentId={}, total={}, success={}, failure={}",
                documentId, result.totalCount(), result.successCount(), result.failureCount());
        return result;
    }

    public List<DocumentChunkPreviewResponse> previewChunks(String documentId) {
        documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在"));
        return chunkRepository.findByDocumentDocumentIdOrderByChunkSeqAsc(documentId).stream()
                .map(DocumentChunkPreviewResponse::from)
                .toList();
    }
}
