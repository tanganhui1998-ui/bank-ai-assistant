package com.bank.aiassistant.document.service;

import com.bank.aiassistant.config.DocumentTaskProperties;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.bank.aiassistant.document.dto.DocumentDownloadUrlResponse;
import com.bank.aiassistant.document.dto.DocumentMetadataRequest;
import com.bank.aiassistant.document.dto.DocumentResponse;
import com.bank.aiassistant.document.dto.DocumentUpdateRequest;
import com.bank.aiassistant.document.dto.DocumentUploadResponse;
import com.bank.aiassistant.document.message.DocumentParseMessage;
import com.bank.aiassistant.document.message.DocumentParseMessageProducer;
import com.bank.aiassistant.domain.entity.Document;
import com.bank.aiassistant.domain.enums.ConfidentialityLevel;
import com.bank.aiassistant.domain.enums.DocumentBusinessType;
import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import com.bank.aiassistant.exception.BusinessException;
import com.bank.aiassistant.oss.OssService;
import com.bank.aiassistant.oss.OssUploadResult;
import com.bank.aiassistant.repository.DocumentRepository;
import com.bank.aiassistant.search.KnowledgeChunkEsService;
import com.bank.aiassistant.util.FileHashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    /**
     * Business-level upload limit. Spring MVC also has multipart limits in
     * application.yml, but this constant keeps the domain rule close to the upload flow.
     */
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    /**
     * Temporary OSS URLs are short-lived because uploaded documents may contain
     * internal bank policies or operating procedures.
     */
    private static final long DOWNLOAD_URL_EXPIRE_SECONDS = 300L;

    /**
     * OSS month partition used in object keys. It keeps object listings manageable
     * and gives later archival jobs a simple time-based partition.
     */
    private static final DateTimeFormatter OSS_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    private final DocumentRepository documentRepository;
    private final OssService ossService;
    private final CurrentUserProvider currentUserProvider;
    private final DocumentParseMessageProducer documentParseMessageProducer;
    private final DocumentTaskProperties documentTaskProperties;
    private final KnowledgeChunkEsService knowledgeChunkEsService;

    /**
     * Uploads a document and creates the master metadata record.
     *
     * Consistency notes:
     * - Database writes are protected by the Spring transaction.
     * - OSS writes are not transactional, so uploaded objects are deleted manually if
     *   database persistence fails afterwards.
     * - The RabbitMQ parse message is sent only after transaction commit, so a fast
     *   consumer never sees a document id before the row exists.
     *
     * Deduplication notes:
     * - SHA-256 is calculated before upload.
     * - If the same content already exists, this method returns the existing
     *   document id and skips OSS upload.
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadResponse upload(MultipartFile file, DocumentMetadataRequest metadata) {
        CurrentUser currentUser = currentUserProvider.currentUser();
        validateFile(file);

        String fileHash = calculateHash(file);
        return documentRepository.findByFileHash(fileHash)
                .map(existing -> {
                    // Idempotent upload: identical file content maps to the same document.
                    log.info("Duplicate document upload detected, userId={}, documentId={}, fileHash={}",
                            currentUser.userId(), existing.getDocumentId(), fileHash);
                    return DocumentUploadResponse.builder()
                            .documentId(existing.getDocumentId())
                            .status(existing.getProcessStatus())
                            .duplicated(true)
                            .build();
                })
                .orElseGet(() -> uploadNewDocument(file, metadata, currentUser, fileHash));
    }

    /**
     * Generates a five-minute OSS signed URL for download or preview.
     */
    @Transactional(readOnly = true)
    public DocumentDownloadUrlResponse generateDownloadUrl(String documentId) {
        Document document = getActiveDocument(documentId);
        URL url = ossService.generateTemporaryUrl(document.getOssObjectKey());
        log.info("Generated temporary download url, documentId={}, objectKey={}", documentId, document.getOssObjectKey());
        return DocumentDownloadUrlResponse.builder()
                .documentId(documentId)
                .url(url.toString())
                .expireSeconds(DOWNLOAD_URL_EXPIRE_SECONDS)
                .build();
    }

    /**
     * Returns complete metadata for a document id.
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在"));
        log.info("Queried document metadata, documentId={}", documentId);
        return DocumentResponse.from(document);
    }

    /**
     * Searches documents by optional filters. Null filter values are ignored.
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(
            DocumentProcessStatus status,
            DocumentBusinessType documentType,
            String department,
            Pageable pageable
    ) {
        long total = documentRepository.countByFilters(status, documentType, department);
        List<DocumentResponse> rows = documentRepository.findByFilters(
                        status,
                        documentType,
                        department,
                        pageable.getPageSize(),
                        pageable.getOffset())
                .stream()
                .map(DocumentResponse::from)
                .toList();
        Page<DocumentResponse> page = new PageImpl<>(rows, pageable, total);
        log.info("Listed documents, status={}, documentType={}, department={}, page={}, size={}",
                status, documentType, department, pageable.getPageNumber(), pageable.getPageSize());
        return page;
    }

    /**
     * Updates business metadata only. File identity, file hash, OSS key and uploader
     * audit fields are intentionally immutable after upload.
     */
    @Transactional
    public DocumentResponse updateMetadata(String documentId, DocumentUpdateRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在"));

        document.setDisplayName(request.getDisplayName());
        document.setDocumentType(request.getDocumentType());
        document.setVersionNo(request.getVersionNo());
        document.setDepartment(request.getDepartment());
        document.setEffectiveTime(request.getEffectiveTime());
        document.setConfidentialityLevel(request.getConfidentialityLevel());
        document.setApplicableScope(request.getApplicableScope());
        document.setPublishingUnit(request.getPublishingUnit());

        Document saved = documentRepository.save(document);
        log.info("Updated document metadata, documentId={}", documentId);
        return DocumentResponse.from(saved);
    }

    /**
     * Soft-deletes a document from business queries while retaining the database row
     * for audit. The physical OSS object is removed immediately.
     */
    @Transactional
    public void deleteDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在"));
        if (document.getProcessStatus() == DocumentProcessStatus.EXPIRED) {
            log.info("Document already expired, documentId={}", documentId);
            return;
        }

        ossService.delete(document.getOssObjectKey());
        document.setProcessStatus(DocumentProcessStatus.EXPIRED);
        documentRepository.save(document);
        knowledgeChunkEsService.markDocumentExpired(document.getDocumentId());
        log.info("Deleted document object and marked expired, documentId={}, objectKey={}",
                documentId, document.getOssObjectKey());
    }

    private DocumentUploadResponse uploadNewDocument(
            MultipartFile file,
            DocumentMetadataRequest metadata,
            CurrentUser currentUser,
            String fileHash
    ) {
        // Generate the id before OSS upload because the id is embedded in the object key.
        String documentId = UUID.randomUUID().toString();
        String objectKey = buildObjectKey(currentUser.tenantId(), documentId, file.getOriginalFilename());
        OssUploadResult uploadResult = null;

        try (InputStream inputStream = file.getInputStream()) {
            // Upload first so the database row can persist bucket/key/ETag metadata.
            uploadResult = ossService.upload(objectKey, inputStream);
            Document document = buildDocument(documentId, file, metadata, currentUser, fileHash, uploadResult);
            // Flush immediately so database errors happen while OSS cleanup is still possible.
            Document saved = documentRepository.save(document);
            registerParseMessageAfterCommit(saved.getDocumentId(), currentUser);
            log.info("Uploaded new document successfully, documentId={}, objectKey={}",
                    saved.getDocumentId(), objectKey);
            return DocumentUploadResponse.builder()
                    .documentId(saved.getDocumentId())
                    .status(saved.getProcessStatus())
                    .duplicated(false)
                    .build();
        } catch (Exception ex) {
            cleanupUploadedObject(uploadResult, ex);
            throw toBusinessException(ex, "DOCUMENT_UPLOAD_FAILED", "文档上传失败");
        }
    }

    /**
     * Sends the parse task after the upload transaction commits.
     *
     * If the message were sent inside the transaction, the consumer could read it
     * before the document row is visible. Sending after commit also makes RabbitMQ
     * failures non-blocking for the upload result; the producer logs failures so a
     * later compensation job can resend messages for UPLOAD_COMPLETED documents.
     */
    private void registerParseMessageAfterCommit(String documentId, CurrentUser currentUser) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                DocumentParseMessage message = new DocumentParseMessage(
                        documentId,
                        currentUser.tenantId(),
                        currentUser.userId(),
                        currentUser.userName(),
                        LocalDateTime.now(),
                        0,
                        documentTaskProperties.getParseMaxRetryCount()
                );
                documentParseMessageProducer.send(message);
            }
        });
    }

    /**
     * Maps request metadata, OSS upload result and current user info into the master
     * document entity.
     */
    private Document buildDocument(
            String documentId,
            MultipartFile file,
            DocumentMetadataRequest metadata,
            CurrentUser currentUser,
            String fileHash,
            OssUploadResult uploadResult
    ) {
        return Document.builder()
                .documentId(documentId)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileType(resolveFileType(file))
                .fileHash(fileHash)
                .ossBucket(uploadResult.bucketName())
                .ossObjectKey(uploadResult.objectKey())
                .ossEtag(uploadResult.eTag())
                .accessUrl(uploadResult.accessUrl())
                .displayName(metadata.getDisplayName())
                .documentType(metadata.getDocumentType())
                .versionNo(metadata.getVersionNo())
                .department(metadata.getDepartment())
                .effectiveTime(metadata.getEffectiveTime())
                .confidentialityLevel(resolveConfidentialityLevel(metadata.getConfidentialityLevel()))
                .applicableScope(metadata.getApplicableScope())
                .publishingUnit(metadata.getPublishingUnit())
                .processStatus(DocumentProcessStatus.UPLOAD_COMPLETED)
                .retryCount(0)
                .uploaderId(currentUser.userId())
                .uploaderName(currentUser.userName())
                .build();
    }

    /**
     * Loads a document that is still allowed to be downloaded or previewed.
     */
    private Document getActiveDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在"));
        if (document.getProcessStatus() == DocumentProcessStatus.EXPIRED) {
            throw new BusinessException("DOCUMENT_EXPIRED", "文档已过期");
        }
        return document;
    }

    /**
     * Performs cheap request validation before hashing or uploading the file.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "文件大小不能超过50MB");
        }
        if (!isAllowedFileType(file)) {
            throw new BusinessException("FILE_TYPE_NOT_SUPPORTED", "文件类型仅支持PDF、DOC、DOCX");
        }
    }

    /**
     * Accepts either MIME type or filename extension. Browsers and office clients
     * are not always consistent for DOC/DOCX MIME values.
     */
    private boolean isAllowedFileType(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        boolean allowedExtension = "pdf".equals(extension) || "doc".equals(extension) || "docx".equals(extension);
        boolean allowedMime = "application/pdf".equalsIgnoreCase(contentType)
                || "application/msword".equalsIgnoreCase(contentType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType);
        return allowedExtension || allowedMime;
    }

    /**
     * Calculates the content hash used for deduplication.
     */
    private String calculateHash(MultipartFile file) {
        try {
            return FileHashUtils.sha256(file);
        } catch (IOException ex) {
            throw new BusinessException("FILE_HASH_FAILED", "文件哈希计算失败");
        }
    }

    /**
     * OSS object key format:
     * knowledge-base/{tenantId}/{yyyy/MM}/{documentId}-{originalFilename}
     */
    private String buildObjectKey(String tenantId, String documentId, String originalFilename) {
        String safeFilename = sanitizeFilename(originalFilename);
        String monthPath = OSS_MONTH_FORMATTER.format(LocalDate.now());
        return "knowledge-base/" + tenantId + "/" + monthPath + "/" + documentId + "-" + safeFilename;
    }

    /**
     * Prevents path separators in user-supplied filenames from changing the OSS
     * directory layout.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        return filename.replace("\\", "_").replace("/", "_").trim();
    }

    /**
     * Stores a concise file type. Extension is preferred for management screens;
     * MIME type is used as a fallback when there is no extension.
     */
    private String resolveFileType(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        if (!extension.isBlank()) {
            return extension.toUpperCase(Locale.ROOT);
        }
        return file.getContentType();
    }

    /**
     * Extracts the lowercase filename extension without the dot.
     */
    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Defaults to INTERNAL because bank documents should not become PUBLIC unless
     * the uploader explicitly selects that level.
     */
    private ConfidentialityLevel resolveConfidentialityLevel(ConfidentialityLevel confidentialityLevel) {
        return confidentialityLevel == null ? ConfidentialityLevel.INTERNAL : confidentialityLevel;
    }

    /**
     * Compensating cleanup for the non-transactional OSS side effect.
     */
    private void cleanupUploadedObject(OssUploadResult uploadResult, Exception originalException) {
        if (uploadResult == null) {
            return;
        }
        try {
            ossService.delete(uploadResult.objectKey());
            log.info("Cleaned OSS object after upload failure, objectKey={}", uploadResult.objectKey());
        } catch (Exception cleanupException) {
            originalException.addSuppressed(cleanupException);
            log.error("Failed to clean OSS object after upload failure, objectKey={}",
                    uploadResult.objectKey(), cleanupException);
        }
    }

    /**
     * Keeps known business exceptions intact and wraps unexpected technical failures
     * in a stable API error code.
     */
    private RuntimeException toBusinessException(Exception ex, String code, String message) {
        if (ex instanceof BusinessException businessException) {
            return businessException;
        }
        log.error("{}: {}", message, ex.getMessage(), ex);
        return new BusinessException(code, message);
    }
}
