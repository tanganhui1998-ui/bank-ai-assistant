package com.bank.aiassistant.security;

import com.bank.aiassistant.config.SecurityComplianceProperties;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.tool.PendingConfirmationRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityRiskAssessmentServiceTest {

    private final SecurityComplianceProperties properties = new SecurityComplianceProperties();
    private final SecurityRiskAssessmentService service = new SecurityRiskAssessmentService(properties);

    @Test
    void checkPendingOperationShouldRejectLongLeave() {
        properties.setMaxLeaveDays(3);
        PendingConfirmationRecord record = new PendingConfirmationRecord(
                "p1",
                "u1",
                "create_leave_application",
                Map.of("startTime", "2026-07-01 09:00", "endTime", "2026-07-10 18:00", "reason", "休假"),
                "创建请假单",
                LocalDateTime.now().plusMinutes(5)
        );

        SecurityDecision decision = service.checkPendingOperation(record);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("合规上限");
    }

    @Test
    void assessShouldMarkRejectedEventAsHighRisk() {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .user(new CurrentUser("u1", "用户", "tenant", List.of("EMPLOYEE"), List.of("科技部")))
                .actionType("CONFIRM_REJECTED")
                .rejected(true)
                .build();

        assertThat(service.assess(event)).isEqualTo("HIGH");
    }
}
