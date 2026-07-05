package com.bank.aiassistant.dialog;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 前端查询会话状态的响应。
 */
@Builder
public record ConversationStatusResponse(
        String conversationId,
        ConversationState state,
        IntentType pendingIntent,
        Map<String, Object> slots,
        List<String> missingSlots,
        String pendingOperationId,
        List<ConversationMessageSnapshot> history
) {
}
