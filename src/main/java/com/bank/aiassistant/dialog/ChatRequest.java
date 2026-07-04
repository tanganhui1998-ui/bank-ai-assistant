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

    @NotBlank(message = "message cannot be blank")
    private String message;
}
