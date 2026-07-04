package com.bank.aiassistant.security;

/**
 * 安全校验结果。
 */
public record SecurityDecision(
        boolean allowed,
        String reason
) {

    public static SecurityDecision allow() {
        return new SecurityDecision(true, null);
    }

    public static SecurityDecision deny(String reason) {
        return new SecurityDecision(false, reason);
    }
}
