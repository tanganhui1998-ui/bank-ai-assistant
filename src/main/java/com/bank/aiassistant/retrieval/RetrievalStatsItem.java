package com.bank.aiassistant.retrieval;

import lombok.Builder;

/**
 * 检索统计项。
 */
@Builder
public record RetrievalStatsItem(
        String question,
        Long queryCount,
        Double avgHitCount
) {
}
