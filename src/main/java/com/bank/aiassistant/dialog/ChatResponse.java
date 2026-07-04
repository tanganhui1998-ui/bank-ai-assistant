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
        IntentType intent,
        String answer,
        List<CitationSource> citations,
        Map<String, Object> slots,
        boolean lowConfidence,
        String routeTarget,
        Long elapsedMs
) {
}
