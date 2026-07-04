package com.bank.aiassistant.security;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 零误操作对账报告。
 */
@Builder
public record SecurityReconciliationReport(
        LocalDateTime generatedTime,
        long confirmedWithoutOrderCount,
        long expiredFixedCount,
        List<String> abnormalPendingIds
) {
}
