package com.bank.aiassistant.llm;

/**
 * Qwen OpenAI-compatible Chat message。
 */
public record QwenChatMessage(
        String role,
        String content
) {
}
