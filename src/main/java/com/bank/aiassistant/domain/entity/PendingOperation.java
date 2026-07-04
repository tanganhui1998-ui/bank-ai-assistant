package com.bank.aiassistant.domain.entity;

import com.bank.aiassistant.domain.enums.PendingOperationStatus;
import com.bank.aiassistant.domain.enums.PendingOperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 写操作待确认记录。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingOperation {

    private String pendingId;
    private String userId;
    private PendingOperationType operationType;
    private String toolName;
    private String businessParamsJson;
    private String operationSummary;
    private PendingOperationStatus status;
    private LocalDateTime expireTime;
    private LocalDateTime createdTime;
    private LocalDateTime confirmedTime;
}
