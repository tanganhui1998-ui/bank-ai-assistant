package com.bank.aiassistant.security;

import com.bank.aiassistant.config.SecurityComplianceProperties;
import com.bank.aiassistant.tool.PendingConfirmationRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * AI 写操作风险评估服务。
 *
 * 这里放置“最后一道门”的业务合规规则。真实生产可继续扩展为规则引擎或接入风控中心。
 */
@Service
@RequiredArgsConstructor
public class SecurityRiskAssessmentService {

    private final SecurityComplianceProperties properties;

    public String assess(SecurityAuditEvent event) {
        if (event.rejected()) {
            return "HIGH";
        }
        if (event.operationType() != null && event.operationType().contains("WRITE")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public SecurityDecision checkPendingOperation(PendingConfirmationRecord record) {
        if (record == null || record.params() == null) {
            return SecurityDecision.allow();
        }
        if ("create_leave_application".equals(record.toolName())) {
            return checkLeaveRisk(record.params());
        }
        if ("submit_approval_opinion".equals(record.toolName()) && containsDangerousText(record.params())) {
            return SecurityDecision.deny("审批意见包含高风险敏感指令，请修改后重新提交。");
        }
        return SecurityDecision.allow();
    }

    private SecurityDecision checkLeaveRisk(Map<String, Object> params) {
        try {
            LocalDateTime start = parseDateTime(String.valueOf(params.get("startTime")));
            LocalDateTime end = parseDateTime(String.valueOf(params.get("endTime")));
            long days = Math.max(1, Duration.between(start, end).toDays() + 1);
            if (days > properties.getMaxLeaveDays()) {
                return SecurityDecision.deny("请假天数超过系统合规上限，请走线下审批流程。");
            }
        } catch (Exception ignored) {
            return SecurityDecision.allow();
        }
        return SecurityDecision.allow();
    }

    private boolean containsDangerousText(Map<String, Object> params) {
        String text = String.valueOf(params);
        return text.contains("绕过") || text.contains("免审") || text.contains("跳过审批") || text.contains("删除日志");
    }

    private LocalDateTime parseDateTime(String value) {
        String normalized = value.length() == 10 ? value + " 00:00" : value;
        return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
