package com.bank.aiassistant.dialog;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class SseEventSenderTest {

    private final SseEventSender sender = new SseEventSender();

    @Test
    void buildEventShouldWrapPayloadWithStableProtocolFields() {
        AtomicLong sequence = new AtomicLong();

        AssistantSseEvent event = sender.buildEvent(
                "message",
                "c-001",
                "m-001",
                sequence,
                Map.of("delta", "你好")
        );

        assertThat(event.type()).isEqualTo("message");
        assertThat(event.conversationId()).isEqualTo("c-001");
        assertThat(event.messageId()).isEqualTo("m-001");
        assertThat(event.sequence()).isEqualTo(1L);
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.timestamp()).isNotNull();
    }
}
