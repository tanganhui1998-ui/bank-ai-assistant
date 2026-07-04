package com.bank.aiassistant.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Function Calling 工具调用审计记录。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiToolCallAudit {

    private String callId;
    private String userId;
    private String toolName;
    private String inputParamsJson;
    private String callResult;
    private Boolean permissionRejected;
    private String rejectReason;
    private LocalDateTime calledTime;
    private Long elapsedMs;
}
