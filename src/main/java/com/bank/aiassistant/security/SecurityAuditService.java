package com.bank.aiassistant.security;

import com.bank.aiassistant.config.SecurityComplianceProperties;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.domain.entity.AiSecurityAuditLog;
import com.bank.aiassistant.repository.AiSecurityAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 全链路安全审计异步写入服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final AiSecurityAuditLogRepository repository;
    private final SensitiveDataMasker masker;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;
    private final SecurityComplianceProperties complianceProperties;
    private final SecurityRiskAssessmentService riskAssessmentService;

    @Async("retrievalExecutor")
    public void record(SecurityAuditEvent event) {
        try {
            CurrentUser user = event.user();
            Map<String, Object> maskedParams = masker.mask(event.inputParams());
            Object maskedResult = masker.maskObject(event.result());
            LocalDateTime now = LocalDateTime.now();
            repository.save(AiSecurityAuditLog.builder()
                    .userId(user.userId())
                    .userName(user.userName())
                    .sessionId(event.sessionId())
                    .actionType(event.actionType())
                    .toolName(event.toolName())
                    .operationType(event.operationType())
                    .inputParamsJson(objectMapper.writeValueAsString(maskedParams))
                    .resultJson(objectMapper.writeValueAsString(maskedResult))
                    .rejected(event.rejected())
                    .rejectReason(event.rejectReason())
                    .traceId(RequestTraceContext.get())
                    .riskLevel(riskAssessmentService.assess(event))
                    .elapsedMs(event.elapsedMs())
                    .clientIp(clientIpResolver.resolve())
                    .createdTime(now)
                    .retentionUntil(now.plusDays(complianceProperties.getAuditRetentionDays()))
                    .build());
        } catch (Exception ex) {
            log.error("Failed to write security audit log, actionType={}, toolName={}",
                    event.actionType(), event.toolName(), ex);
        }
    }
}
