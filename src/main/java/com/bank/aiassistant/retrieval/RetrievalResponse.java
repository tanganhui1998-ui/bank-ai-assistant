package com.bank.aiassistant.retrieval;

import lombok.Builder;

import java.util.List;

/**
 * AI 助手在线检索响应。
 */
@Builder
public record RetrievalResponse(
        String question,
        List<RetrievalResultItem> results,
        List<String> citations,
        boolean lowConfidence,
        String message,
        long elapsedMs,
        RetrievalTrace trace
) {
}
