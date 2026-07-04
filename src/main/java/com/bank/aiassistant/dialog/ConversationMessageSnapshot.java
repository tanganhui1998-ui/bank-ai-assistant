package com.bank.aiassistant.dialog;

import java.time.LocalDateTime;

/**
 * Redis 中保存的轻量消息历史。
 *
 * 只保存模型上下文需要的最近消息，不替代数据库中的完整会话审计记录。
 */
public record ConversationMessageSnapshot(
        String role,
        String content,
        LocalDateTime time
) {
}
