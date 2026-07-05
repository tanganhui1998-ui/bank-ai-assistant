package com.bank.aiassistant.document.controller;

import com.bank.aiassistant.document.dto.DocumentDownloadUrlResponse;
import com.bank.aiassistant.document.dto.DocumentChunkPreviewResponse;
import com.bank.aiassistant.document.dto.DocumentMetadataRequest;
import com.bank.aiassistant.document.dto.DocumentResponse;
import com.bank.aiassistant.document.dto.DocumentUpdateRequest;
import com.bank.aiassistant.document.dto.DocumentUploadResponse;
import com.bank.aiassistant.document.service.KnowledgeMaintenanceService;
import com.bank.aiassistant.document.service.DocumentService;
import com.bank.aiassistant.document.service.DocumentVersionService;
import com.bank.aiassistant.domain.enums.DocumentBusinessType;
import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import com.bank.aiassistant.search.BulkIndexResult;
import com.bank.aiassistant.web.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentVersionService documentVersionService;
    private final KnowledgeMaintenanceService knowledgeMaintenanceService;

    /**
     * Uploads a PDF/DOC/DOCX policy document.
     *
     * Request format:
     * - multipart/form-data
     * - part "file": binary document file
     * - part "metadata": JSON matching DocumentMetadataRequest
     *
     * The service layer performs deduplication, OSS upload, database persistence and
     * async parse-message publishing.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<DocumentUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") DocumentMetadataRequest metadata
    ) {
        log.info("Received document upload request, filename={}", file == null ? null : file.getOriginalFilename());
        return Result.success(documentService.upload(file, metadata));
    }

    /**
     * Generates a temporary OSS signed URL for download or preview.
     */
    @GetMapping("/{documentId}/download-url")
    public Result<DocumentDownloadUrlResponse> generateDownloadUrl(@PathVariable String documentId) {
        log.info("Received document download url request, documentId={}", documentId);
        return Result.success(documentService.generateDownloadUrl(documentId));
    }

    /**
     * Returns complete document metadata by id.
     */
    @GetMapping("/{documentId}")
    public Result<DocumentResponse> getDocument(@PathVariable String documentId) {
        log.info("Received document metadata query request, documentId={}", documentId);
        return Result.success(documentService.getDocument(documentId));
    }

    /**
     * Paged document search. Filters are optional and can be combined.
     */
    @GetMapping
    public Result<Page<DocumentResponse>> listDocuments(
            @RequestParam(required = false) DocumentProcessStatus status,
            @RequestParam(required = false) DocumentBusinessType documentType,
            @RequestParam(required = false) String department,
            @PageableDefault(size = 20, sort = "createdTime") Pageable pageable
    ) {
        log.info("Received document list request, status={}, documentType={}, department={}",
                status, documentType, department);
        return Result.success(documentService.listDocuments(status, documentType, department, pageable));
    }

    /**
     * 查询同一文档名称下的所有历史版本。
     */
    @GetMapping("/versions")
    public Result<java.util.List<DocumentResponse>> listVersions(@RequestParam String displayName) {
        log.info("Received document version list request, displayName={}", displayName);
        return Result.success(documentVersionService.listVersions(displayName));
    }

    /**
     * 查询待发布列表，即所有 PARSE_COMPLETED 状态的文档。
     */
    @GetMapping("/pending-publish")
    public Result<java.util.List<DocumentResponse>> listPendingPublish() {
        log.info("Received pending publish document list request");
        return Result.success(documentVersionService.listPendingPublish());
    }

    /**
     * 管理员审核后发布指定版本。
     */
    @PostMapping("/{documentId}/publish")
    public Result<DocumentResponse> publish(@PathVariable String documentId) {
        log.info("Received document publish request, documentId={}", documentId);
        return Result.success(documentVersionService.publish(documentId));
    }

    /**
     * 废弃指定版本。
     */
    @PostMapping("/{documentId}/expire")
    public Result<DocumentResponse> expire(@PathVariable String documentId) {
        log.info("Received document expire request, documentId={}", documentId);
        return Result.success(documentVersionService.expire(documentId));
    }

    /**
     * Updates business metadata. The file itself, hash and OSS object key are not
     * changed by this endpoint.
     */
    @PutMapping("/{documentId}")
    public Result<DocumentResponse> updateMetadata(
            @PathVariable String documentId,
            @Valid @RequestBody DocumentUpdateRequest request
    ) {
        log.info("Received document metadata update request, documentId={}", documentId);
        return Result.success(documentService.updateMetadata(documentId, request));
    }

    /**
     * Deletes the physical OSS file and marks the database row as EXPIRED.
     */
    @DeleteMapping("/{documentId}")
    public Result<Void> deleteDocument(@PathVariable String documentId) {
        log.info("Received document delete request, documentId={}", documentId);
        documentService.deleteDocument(documentId);
        return Result.success(null);
    }

    /**
     * 管理端补偿接口：重新发送文档解析任务。
     */
    @PostMapping("/{documentId}/parse/retry")
    public Result<DocumentResponse> retryParse(@PathVariable String documentId) {
        log.info("Received document parse retry request, documentId={}", documentId);
        return Result.success(knowledgeMaintenanceService.retryParse(documentId));
    }

    /**
     * 管理端补偿接口：基于数据库有效切片重建 ES 索引。
     */
    @PostMapping("/{documentId}/index/rebuild")
    public Result<BulkIndexResult> rebuildIndex(@PathVariable String documentId) {
        log.info("Received document index rebuild request, documentId={}", documentId);
        return Result.success(knowledgeMaintenanceService.rebuildDocumentIndex(documentId));
    }

    /**
     * 管理端预览接口：查看文档切片、质量分和过滤状态。
     */
    @GetMapping("/{documentId}/chunks")
    public Result<java.util.List<DocumentChunkPreviewResponse>> previewChunks(@PathVariable String documentId) {
        log.info("Received document chunk preview request, documentId={}", documentId);
        return Result.success(knowledgeMaintenanceService.previewChunks(documentId));
    }
}
