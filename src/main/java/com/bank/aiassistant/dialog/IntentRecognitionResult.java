package com.bank.aiassistant.dialog;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 意图识别结构化结果。
 */
@Builder
public record IntentRecognitionResult(
        IntentType intent,
        double confidence,
        Map<String, Object> slots,
        List<String> missingSlots,
        String clarifyQuestion,
        String routeTarget,
        String reason,
        boolean fallback
) {
}
