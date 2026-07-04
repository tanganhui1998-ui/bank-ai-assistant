package com.bank.aiassistant.dialog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SSE 事件发送工具。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventSender {

    public void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException ex) {
            log.warn("SSE send failed, eventName={}", eventName, ex);
            throw new IllegalStateException("SSE send failed", ex);
        }
    }
}
