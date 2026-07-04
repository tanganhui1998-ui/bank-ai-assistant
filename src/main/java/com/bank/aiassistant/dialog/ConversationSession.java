package com.bank.aiassistant.dialog;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Redis 会话状态快照。
 */
@Builder
public record ConversationSession(
        String sessionId,
        ConversationState state,
        IntentType pendingIntent,
        String currentIntent,
        Map<String, Object> slots,
        List<String> missingSlots,
        String pendingOperationId,
        List<ConversationMessageSnapshot> history
) {
}
