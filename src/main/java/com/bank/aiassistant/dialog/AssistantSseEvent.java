package com.bank.aiassistant.dialog;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 前端统一消费的 SSE 事件协议。
 *
 * 所有 text/event-stream 事件都使用该 envelope，前端可以稳定依赖 type、
 * conversationId、messageId 和 sequence 做渲染、去重与断线排查。
 */
@Builder
public record AssistantSseEvent(
        String eventId,
        String type,
        String conversationId,
        String messageId,
        Long sequence,
        Object payload,
        LocalDateTime timestamp
) {
}
