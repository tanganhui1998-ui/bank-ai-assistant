package com.bank.aiassistant.security;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 安全审计查询响应。
 */
@Builder
public record SecurityAuditResponse(
        String auditId,
        String userId,
        String userName,
        String sessionId,
        String actionType,
        String toolName,
        String operationType,
        String inputParamsJson,
        String resultJson,
        Boolean rejected,
        String rejectReason,
        String traceId,
        String riskLevel,
        Long elapsedMs,
        String clientIp,
        LocalDateTime createdTime,
        LocalDateTime retentionUntil
) {
}
