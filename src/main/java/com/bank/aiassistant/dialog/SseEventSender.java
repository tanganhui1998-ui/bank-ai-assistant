package com.bank.aiassistant.dialog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE 事件发送工具。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventSender {

    public void send(SseEmitter emitter, String eventName, Object data) {
        send(emitter, eventName, null, null, null, data);
    }

    public void send(
            SseEmitter emitter,
            String eventName,
            String conversationId,
            String messageId,
            AtomicLong sequence,
            Object data
    ) {
        try {
            AssistantSseEvent event = buildEvent(eventName, conversationId, messageId, sequence, data);
            emitter.send(SseEmitter.event()
                    .id(event.eventId())
                    .name(eventName)
                    .data(event));
        } catch (IOException ex) {
            log.warn("SSE send failed, eventName={}", eventName, ex);
            throw new IllegalStateException("SSE send failed", ex);
        }
    }

    AssistantSseEvent buildEvent(
            String eventName,
            String conversationId,
            String messageId,
            AtomicLong sequence,
            Object data
    ) {
        long seq = sequence == null ? 0L : sequence.incrementAndGet();
        return AssistantSseEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(eventName)
                .conversationId(conversationId)
                .messageId(messageId)
                .sequence(seq)
                .payload(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
