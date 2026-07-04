package com.bank.aiassistant.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 在线检索审计日志。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalAuditLog {

    private String logId;
    private String userId;
    private String userName;
    private String question;
    private String questionHash;
    private Integer hitCount;
    private Long elapsedMs;
    private String documentIds;
    private Double maxScore;
    private Boolean lowConfidence;
    private LocalDateTime createdTime;
}
