package com.bank.aiassistant.tool;

import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.domain.entity.PendingOperation;
import com.bank.aiassistant.domain.enums.PendingOperationStatus;
import com.bank.aiassistant.domain.enums.PendingOperationType;
import com.bank.aiassistant.repository.PendingOperationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 待确认操作服务。
 *
 * WRITE 工具不会直接执行业务写入，而是同时写入数据库审计记录和 Redis 确认卡片。
 * Redis 记录 TTL 为 5 分钟，用于前端确认/取消；数据库记录用于长期审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingConfirmationService {

    private static final String CONFIRM_PREFIX = "ai:pending-confirm:";
    private static final Duration CONFIRM_TTL = Duration.ofMinutes(5);

    private final PendingOperationRepository repository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public String create(CurrentUser user, PendingOperationType type, String summary, Map<String, Object> params) {
        return create(user, type.name(), type, summary, params);
    }

    public String create(CurrentUser user, String toolName, PendingOperationType type, String summary, Map<String, Object> params) {
        try {
            LocalDateTime expireTime = LocalDateTime.now().plusMinutes(5);
            Map<String, Object> safeParams = params == null ? Map.of() : params;
            PendingOperation saved = repository.save(PendingOperation.builder()
                    .userId(user.userId())
                    .operationType(type)
                    .toolName(toolName)
                    .businessParamsJson(objectMapper.writeValueAsString(safeParams))
                    .operationSummary(summary)
                    .status(PendingOperationStatus.PENDING_CONFIRM)
                    .expireTime(expireTime)
                    .createdTime(LocalDateTime.now())
                    .build());

            PendingConfirmationRecord record = new PendingConfirmationRecord(
                    saved.getPendingId(),
                    user.userId(),
                    toolName,
                    safeParams,
                    summary,
                    expireTime
            );
            redisTemplate.opsForValue().set(
                    confirmKey(user.userId(), saved.getPendingId()),
                    objectMapper.writeValueAsString(record),
                    CONFIRM_TTL
            );
            return saved.getPendingId();
        } catch (Exception ex) {
            throw new IllegalStateException("创建待确认操作失败", ex);
        }
    }

    public PendingConfirmationRecord get(String userId, String pendingId) {
        try {
            String json = redisTemplate.opsForValue().get(confirmKey(userId, pendingId));
            return json == null ? null : objectMapper.readValue(json, PendingConfirmationRecord.class);
        } catch (Exception ex) {
            log.error("读取待确认操作失败, userId={}, pendingId={}", userId, pendingId, ex);
            return null;
        }
    }

    public void remove(String userId, String pendingId) {
        redisTemplate.delete(confirmKey(userId, pendingId));
    }

    private String confirmKey(String userId, String pendingId) {
        return CONFIRM_PREFIX + userId + ":" + pendingId;
    }
}
