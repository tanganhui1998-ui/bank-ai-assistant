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

    @NotBlank(message = "message cannot be blank")
    private String message;
}
