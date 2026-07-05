package com.bank.aiassistant.business;

import lombok.Builder;

import java.util.Map;

/**
 * 真实业务系统写操作执行结果。
 */
@Builder
public record BusinessExecutionResult(
        boolean success,
        String businessOrderNo,
        String message,
        Map<String, Object> data
) {
}
