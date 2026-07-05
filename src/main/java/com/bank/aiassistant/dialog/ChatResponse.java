package com.bank.aiassistant.dialog;

import com.bank.aiassistant.retrieval.CitationSource;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 对话入口响应。
 */
@Builder
public record ChatResponse(
        String sessionId,
        String messageId,
        IntentType intent,
        String answer,
        List<CitationSource> citations,
        Map<String, Object> slots,
        ConversationState conversationState,
        String pendingOperationId,
        List<String> suggestedActions,
        boolean lowConfidence,
        String routeTarget,
        Long elapsedMs
) {
}
