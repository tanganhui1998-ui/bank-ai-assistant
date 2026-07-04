package com.bank.aiassistant.document.service;

import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.bank.aiassistant.document.dto.DocumentResponse;
import com.bank.aiassistant.domain.entity.Document;
import com.bank.aiassistant.domain.entity.DocumentChunk;
import com.bank.aiassistant.domain.enums.ChunkStatus;
import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import com.bank.aiassistant.exception.BusinessException;
import com.bank.aiassistant.repository.DocumentChunkRepository;
import com.bank.aiassistant.repository.DocumentRepository;
import com.bank.aiassistant.search.KnowledgeChunkEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档版本与发布管理服务。
 *
 * 同一个 displayName 可以存在多个版本，versionNo 用于区分业务版本。
 * 发布新版本时，同名旧版本会标记为 EXPIRED，ES 中旧版本切片也会被标记为过期，
 * 从而保证检索默认只命中最新已发布版本。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVersionService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeChunkEsService knowledgeChunkEsService;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public List<DocumentResponse> listVersions(String displayName) {
        return documentRepository.findByDisplayNameOrderByCreatedTimeDesc(displayName).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listPendingPublish() {
        return documentRepository.findByProcessStatusOrderByCreatedTimeDesc(DocumentProcessStatus.PARSE_COMPLETED).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /**
     * 发布指定版本。
     *
     * 只有解析完成的文档可以发布。发布时会废弃同名旧版本，并同步更新 ES 中的
     * status/latestVersion 字段，保证检索层默认只看到最新发布版本。
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'KNOWLEDGE_MANAGER')")
    @Transactional
    public DocumentResponse publish(String documentId) {
        CurrentUser currentUser = currentUserProvider.currentUser();
        Document target = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "Document not found"));
        if (target.getProcessStatus() != DocumentProcessStatus.PARSE_COMPLETED
                && target.getProcessStatus() != DocumentProcessStatus.PUBLISHED) {
            throw new BusinessException("DOCUMENT_NOT_READY", "Only parsed documents can be published");
        }

        List<Document> oldPublishedVersions = documentRepository
                .findByDisplayNameAndProcessStatusOrderByPublishedTimeDesc(
                        target.getDisplayName(), DocumentProcessStatus.PUBLISHED)
                .stream()
                .filter(document -> !document.getDocumentId().equals(target.getDocumentId()))
                .toList();

        for (Document oldVersion : oldPublishedVersions) {
            expireDocumentVersion(oldVersion);
        }

        target.setProcessStatus(DocumentProcessStatus.PUBLISHED);
        target.setPublishedTime(LocalDateTime.now());
        Document saved = documentRepository.save(target);
        knowledgeChunkEsService.markDocumentPublished(saved.getDocumentId());
        log.info("Published document version, operatorId={}, operatorName={}, documentId={}, displayName={}, versionNo={}",
                currentUser.userId(), currentUser.userName(), saved.getDocumentId(), saved.getDisplayName(), saved.getVersionNo());
        return DocumentResponse.from(saved);
    }

    /**
     * 废弃指定版本。废弃后 DB 文档状态为 EXPIRED，DB 切片状态为 EXPIRED，
     * ES 切片 status=EXPIRED/latestVersion=false。
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'KNOWLEDGE_MANAGER')")
    @Transactional
    public DocumentResponse expire(String documentId) {
        CurrentUser currentUser = currentUserProvider.currentUser();
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "Document not found"));
        expireDocumentVersion(document);
        log.info("Expired document version, operatorId={}, operatorName={}, documentId={}, displayName={}, versionNo={}",
                currentUser.userId(), currentUser.userName(), document.getDocumentId(), document.getDisplayName(), document.getVersionNo());
        return DocumentResponse.from(document);
    }

    private void expireDocumentVersion(Document document) {
        document.setProcessStatus(DocumentProcessStatus.EXPIRED);
        documentRepository.save(document);

        List<DocumentChunk> chunks = chunkRepository.findByDocumentDocumentIdOrderByChunkSeqAsc(document.getDocumentId());
        for (DocumentChunk chunk : chunks) {
            chunk.setStatus(ChunkStatus.EXPIRED);
        }
        chunkRepository.saveAll(chunks);
        knowledgeChunkEsService.markDocumentExpired(document.getDocumentId());
    }
}
