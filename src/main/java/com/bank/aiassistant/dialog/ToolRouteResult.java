package com.bank.aiassistant.dialog;

import lombok.Builder;

import java.util.Map;

/**
 * 工具调用路由结果。
 */
@Builder
public record ToolRouteResult(
        String answer,
        Map<String, Object> data,
        String pendingOperationId,
        boolean waitingConfirm
) {
}
