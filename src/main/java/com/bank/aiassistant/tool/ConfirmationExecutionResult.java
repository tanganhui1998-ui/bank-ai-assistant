package com.bank.aiassistant.tool;

import lombok.Builder;

import java.util.Map;

/**
 * 二次确认执行结果。
 */
@Builder
public record ConfirmationExecutionResult(
        boolean success,
        boolean canceled,
        String message,
        Map<String, Object> data
) {
}
