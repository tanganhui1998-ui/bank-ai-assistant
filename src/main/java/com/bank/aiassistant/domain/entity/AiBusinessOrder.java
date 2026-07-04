package com.bank.aiassistant.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 确认执行后生成的业务订单对账记录。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiBusinessOrder {

    private String orderId;
    private String pendingId;
    private String businessOrderNo;
    private String userId;
    private String toolName;
    private String status;
    private LocalDateTime createdTime;
}
