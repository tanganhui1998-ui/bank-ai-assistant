package com.bank.aiassistant.security;

import com.bank.aiassistant.context.CurrentUser;
import lombok.Builder;

import java.util.Map;

/**
 * 全链路安全审计事件。
 */
@Builder
public record SecurityAuditEvent(
        CurrentUser user,
        String sessionId,
        String actionType,
        String toolName,
        String operationType,
        Map<String, Object> inputParams,
        Object result,
        boolean rejected,
        String rejectReason,
        long elapsedMs
) {
}
