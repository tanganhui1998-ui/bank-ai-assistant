package com.bank.aiassistant.dialog;

/**
 * Redis 中维护的会话状态。
 */
public enum ConversationState {
    NORMAL,
    COLLECTING_SLOTS,
    WAITING_SLOT,
    WAITING_CONFIRM,
    COMPLETED
}
