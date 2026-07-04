package com.bank.aiassistant.search;

/**
 * ES 批量写入结果。
 */
public record BulkIndexResult(
        int totalCount,
        int successCount,
        int failureCount
) {

    public boolean hasFailure() {
        return failureCount > 0;
    }
}
