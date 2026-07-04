package com.bank.aiassistant.dialog;

import java.util.Map;

public interface ConversationMemoryService {

    ConversationSession load(String sessionId);

    void save(ConversationSession session);

    void updateState(String sessionId, ConversationState state, IntentType pendingIntent, Map<String, Object> slots);

    void updateState(String sessionId, ConversationState state, IntentType pendingIntent, Map<String, Object> slots, java.util.List<String> missingSlots, String pendingOperationId);

    void appendMessage(String sessionId, String role, String content);

    IntentRecognitionResult getCachedIntent(String sessionId, String question);

    void cacheIntent(String sessionId, String question, IntentRecognitionResult result);
}
