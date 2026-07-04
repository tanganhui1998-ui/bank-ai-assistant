package com.bank.aiassistant.document.dto;

import com.bank.aiassistant.domain.entity.Document;
import com.bank.aiassistant.domain.enums.ConfidentialityLevel;
import com.bank.aiassistant.domain.enums.DocumentBusinessType;
import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DocumentResponse(
        String documentId,
        String fileName,
        Long fileSize,
        String fileType,
        String fileHash,
        String ossBucket,
        String ossObjectKey,
        String ossEtag,
        String accessUrl,
        String displayName,
        DocumentBusinessType documentType,
        String versionNo,
        String department,
        LocalDateTime effectiveTime,
        ConfidentialityLevel confidentialityLevel,
        String applicableScope,
        String publishingUnit,
        DocumentProcessStatus processStatus,
        String parseErrorMessage,
        Integer retryCount,
        String uploaderId,
        String uploaderName,
        LocalDateTime createdTime,
        LocalDateTime updatedTime,
        LocalDateTime publishedTime
) {

    public static DocumentResponse from(Document document) {
        return DocumentResponse.builder()
                .documentId(document.getDocumentId())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .fileHash(document.getFileHash())
                .ossBucket(document.getOssBucket())
                .ossObjectKey(document.getOssObjectKey())
                .ossEtag(document.getOssEtag())
                .accessUrl(document.getAccessUrl())
                .displayName(document.getDisplayName())
                .documentType(document.getDocumentType())
                .versionNo(document.getVersionNo())
                .department(document.getDepartment())
                .effectiveTime(document.getEffectiveTime())
                .confidentialityLevel(document.getConfidentialityLevel())
                .applicableScope(document.getApplicableScope())
                .publishingUnit(document.getPublishingUnit())
                .processStatus(document.getProcessStatus())
                .parseErrorMessage(document.getParseErrorMessage())
                .retryCount(document.getRetryCount())
                .uploaderId(document.getUploaderId())
                .uploaderName(document.getUploaderName())
                .createdTime(document.getCreatedTime())
                .updatedTime(document.getUpdatedTime())
                .publishedTime(document.getPublishedTime())
                .build();
    }
}
