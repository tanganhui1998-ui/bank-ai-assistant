package com.bank.aiassistant.dialog;

import com.bank.aiassistant.config.DialogProperties;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Redis 会话记忆服务。
 *
 * 当前批次只维护路由所需状态：是否等待补槽、是否等待确认、待处理槽位和意图缓存。
 * 第十批接入完整会话记忆时可以在此类扩展更多字段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisConversationMemoryService implements ConversationMemoryService {

    private static final String SESSION_PREFIX = "ai:dialog:session:";
    private static final String INTENT_PREFIX = "ai:dialog:intent:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final int MAX_HISTORY_SIZE = 20;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DialogProperties properties;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public ConversationSession load(String sessionId) {
        try {
            String json = redisTemplate.opsForValue().get(sessionKey(sessionId));
            if (json == null || json.isBlank()) {
                return ConversationSession.builder()
                        .sessionId(sessionId)
                        .state(ConversationState.NORMAL)
                        .slots(Map.of())
                        .missingSlots(List.of())
                        .history(List.of())
                        .build();
            }
            return objectMapper.readValue(json, ConversationSession.class);
        } catch (Exception ex) {
            log.warn("Failed to load conversation session from Redis, sessionId={}", sessionId, ex);
            return ConversationSession.builder().sessionId(sessionId).state(ConversationState.NORMAL).slots(Map.of()).missingSlots(List.of()).history(List.of()).build();
        }
    }

    @Override
    public void save(ConversationSession session) {
        try {
            redisTemplate.opsForValue().set(sessionKey(session.sessionId()), objectMapper.writeValueAsString(session), SESSION_TTL);
        } catch (Exception ex) {
            log.warn("Failed to save conversation session to Redis, sessionId={}", session.sessionId(), ex);
        }
    }

    @Override
    public void updateState(String sessionId, ConversationState state, IntentType pendingIntent, Map<String, Object> slots) {
        updateState(sessionId, state, pendingIntent, slots, List.of(), null);
    }

    @Override
    public void updateState(
            String sessionId,
            ConversationState state,
            IntentType pendingIntent,
            Map<String, Object> slots,
            List<String> missingSlots,
            String pendingOperationId
    ) {
        ConversationSession existing = load(sessionId);
        save(ConversationSession.builder()
                .sessionId(sessionId)
                .state(state)
                .pendingIntent(pendingIntent)
                .currentIntent(pendingIntent == null ? null : pendingIntent.name())
                .slots(slots == null ? Map.of() : slots)
                .missingSlots(missingSlots == null ? List.of() : missingSlots)
                .pendingOperationId(pendingOperationId)
                .history(existing.history() == null ? List.of() : existing.history())
                .build());
    }

    @Override
    public void appendMessage(String sessionId, String role, String content) {
        ConversationSession existing = load(sessionId);
        List<ConversationMessageSnapshot> history = new ArrayList<>(existing.history() == null ? List.of() : existing.history());
        history.add(new ConversationMessageSnapshot(role, content, LocalDateTime.now()));
        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
        }
        save(ConversationSession.builder()
                .sessionId(sessionId)
                .state(existing.state())
                .pendingIntent(existing.pendingIntent())
                .currentIntent(existing.currentIntent())
                .slots(existing.slots() == null ? Map.of() : existing.slots())
                .missingSlots(existing.missingSlots() == null ? List.of() : existing.missingSlots())
                .pendingOperationId(existing.pendingOperationId())
                .history(history)
                .build());
    }

    @Override
    public IntentRecognitionResult getCachedIntent(String sessionId, String question) {
        try {
            String json = redisTemplate.opsForValue().get(intentKey(sessionId, question));
            return json == null ? null : objectMapper.readValue(json, IntentRecognitionResult.class);
        } catch (Exception ex) {
            log.warn("Failed to read cached intent, sessionId={}", sessionId, ex);
            return null;
        }
    }

    @Override
    public void cacheIntent(String sessionId, String question, IntentRecognitionResult result) {
        try {
            redisTemplate.opsForValue().set(
                    intentKey(sessionId, question),
                    objectMapper.writeValueAsString(result),
                    Duration.ofSeconds(properties.getIntentCacheTtlSeconds())
            );
        } catch (Exception ex) {
            log.warn("Failed to cache intent, sessionId={}", sessionId, ex);
        }
    }

    private String intentKey(String sessionId, String question) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String hash = HexFormat.of().formatHex(digest.digest(question.getBytes(StandardCharsets.UTF_8)));
        return INTENT_PREFIX + currentUserProvider.currentUser().userId() + ":" + sessionId + ":" + hash;
    }

    private String sessionKey(String sessionId) {
        return SESSION_PREFIX + currentUserProvider.currentUser().userId() + ":" + sessionId;
    }
}
