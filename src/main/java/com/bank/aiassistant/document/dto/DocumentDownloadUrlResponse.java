package com.bank.aiassistant.document.dto;

import lombok.Builder;

@Builder
public record DocumentDownloadUrlResponse(
        String documentId,
        String url,
        long expireSeconds
) {
}
