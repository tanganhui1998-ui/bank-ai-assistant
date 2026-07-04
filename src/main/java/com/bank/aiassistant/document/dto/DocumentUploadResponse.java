package com.bank.aiassistant.document.dto;

import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import lombok.Builder;

@Builder
public record DocumentUploadResponse(
        String documentId,
        DocumentProcessStatus status,
        boolean duplicated
) {
}
