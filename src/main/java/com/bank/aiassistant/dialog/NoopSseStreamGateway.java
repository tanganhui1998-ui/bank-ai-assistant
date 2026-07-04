package com.bank.aiassistant.dialog;

import org.springframework.stereotype.Component;

/**
 * SSE 空实现，当前批次不实际推送。
 */
@Component
public class NoopSseStreamGateway implements SseStreamGateway {

    @Override
    public void emit(String sessionId, String content) {
        // 第十批接入 SSE 后替换为真实推送逻辑。
    }
}
