package com.bank.aiassistant.tool;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Redis 中保存的待确认操作卡片。
 */
public record PendingConfirmationRecord(
        String pendingId,
        String userId,
        String toolName,
        Map<String, Object> params,
        String summary,
        LocalDateTime expireTime
) {
}
