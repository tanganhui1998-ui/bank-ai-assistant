package com.bank.aiassistant.retrieval;

import lombok.Builder;

/**
 * RAG 检索反馈响应。
 */
@Builder
public record RetrievalFeedbackResponse(
        String feedbackId,
        String status
) {
}
