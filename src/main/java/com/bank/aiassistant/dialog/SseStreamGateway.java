package com.bank.aiassistant.dialog;

/**
 * SSE 流式返回预留接口。
 *
 * 第十批实现流式响应后，对话服务可以在生成答案时调用该接口推送 token。
 */
public interface SseStreamGateway {

    void emit(String sessionId, String content);
}
