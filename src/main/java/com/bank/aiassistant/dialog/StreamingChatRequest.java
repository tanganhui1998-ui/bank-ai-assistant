package com.bank.aiassistant.dialog;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 流式对话请求。
 */
@Getter
@Setter
public class StreamingChatRequest {

    private String conversationId;

    /**
     * 前端生成的消息 ID，用于幂等、链路追踪和断线排查。
     */
    private String clientMessageId;

    @NotBlank(message = "message cannot be blank")
    private String message;
}
