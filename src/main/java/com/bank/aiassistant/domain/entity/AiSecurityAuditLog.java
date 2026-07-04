package com.bank.aiassistant.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 助手全链路安全审计日志。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSecurityAuditLog {

    private String auditId;
    private String userId;
    private String userName;
    private String sessionId;
    private String actionType;
    private String toolName;
    private String operationType;
    private String inputParamsJson;
    private String resultJson;
    private Boolean rejected;
    private String rejectReason;
    private Long elapsedMs;
    private String clientIp;
    private LocalDateTime createdTime;
}
