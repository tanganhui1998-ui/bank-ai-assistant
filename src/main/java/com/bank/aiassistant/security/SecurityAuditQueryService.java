package com.bank.aiassistant.security;

import com.bank.aiassistant.domain.entity.AiSecurityAuditLog;
import com.bank.aiassistant.repository.AiSecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 安全审计查询服务。
 *
 * 查询接口只面向管理员和安全审计角色开放，返回内容已经在写入时完成脱敏。
 */
@Service
@RequiredArgsConstructor
public class SecurityAuditQueryService {

    private final AiSecurityAuditLogRepository repository;

    public List<SecurityAuditResponse> recentByUser(String userId, int limit) {
        return repository.findRecentByUserId(userId, normalizeLimit(limit)).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<SecurityAuditResponse> recentByRiskLevel(String riskLevel, int limit) {
        return repository.findRecentByRiskLevel(riskLevel, normalizeLimit(limit)).stream()
                .map(this::toResponse)
                .toList();
    }

    private int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 200);
    }

    private SecurityAuditResponse toResponse(AiSecurityAuditLog log) {
        return SecurityAuditResponse.builder()
                .auditId(log.getAuditId())
                .userId(log.getUserId())
                .userName(log.getUserName())
                .sessionId(log.getSessionId())
                .actionType(log.getActionType())
                .toolName(log.getToolName())
                .operationType(log.getOperationType())
                .inputParamsJson(log.getInputParamsJson())
                .resultJson(log.getResultJson())
                .rejected(log.getRejected())
                .rejectReason(log.getRejectReason())
                .traceId(log.getTraceId())
                .riskLevel(log.getRiskLevel())
                .elapsedMs(log.getElapsedMs())
                .clientIp(log.getClientIp())
                .createdTime(log.getCreatedTime())
                .retentionUntil(log.getRetentionUntil())
                .build();
    }
}
