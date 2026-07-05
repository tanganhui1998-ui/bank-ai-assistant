package com.bank.aiassistant.dialog;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 对话入口请求。
 */
@Getter
@Setter
public class ChatRequest {

    private String sessionId;

    /**
     * 前端消息 ID，非流式接口同样返回，方便前端统一关联请求和响应。
     */
    private String clientMessageId;

    @NotBlank(message = "message cannot be blank")
    private String message;
}
