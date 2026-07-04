package com.bank.aiassistant.tool;

import lombok.Builder;

import java.util.Map;

/**
 * 业务工具统一返回结构。
 *
 * 大模型拿到该结构后，可以根据 success/errorCode/message 判断是否继续追问，
 * 或者把 data 中的业务数据整理成自然语言答复。
 */
@Builder
public record ToolCallResult(
        boolean success,
        String errorCode,
        String message,
        Map<String, Object> data,
        boolean permissionRejected,
        String pendingOperationId
) {

    public static ToolCallResult success(String message, Map<String, Object> data) {
        return ToolCallResult.builder()
                .success(true)
                .message(message)
                .data(data == null ? Map.of() : data)
                .permissionRejected(false)
                .build();
    }

    public static ToolCallResult validationError(String message, Map<String, Object> data) {
        return ToolCallResult.builder()
                .success(false)
                .errorCode("VALIDATION_ERROR")
                .message(message)
                .data(data == null ? Map.of() : data)
                .permissionRejected(false)
                .build();
    }

    public static ToolCallResult permissionDenied(String message) {
        return ToolCallResult.builder()
                .success(false)
                .errorCode("PERMISSION_DENIED")
                .message(message)
                .data(Map.of())
                .permissionRejected(true)
                .build();
    }

    public static ToolCallResult pendingConfirm(String message, String pendingOperationId, Map<String, Object> data) {
        return ToolCallResult.builder()
                .success(true)
                .message(message)
                .data(data == null ? Map.of() : data)
                .permissionRejected(false)
                .pendingOperationId(pendingOperationId)
                .build();
    }
}
