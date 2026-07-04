package com.bank.aiassistant.retrieval;

import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.domain.entity.RetrievalAuditLog;
import com.bank.aiassistant.repository.RetrievalAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * 检索审计异步写入服务。
 *
 * 在线检索链路要求低延迟，审计日志不能阻塞主响应，因此使用 @Async 写入数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalAuditService {

    private final RetrievalAuditLogRepository repository;

    @Async("retrievalExecutor")
    public void record(CurrentUser user, RetrievalResponse response) {
        try {
            List<String> documentIds = response.results().stream()
                    .map(RetrievalResultItem::documentId)
                    .distinct()
                    .toList();
            Double maxScore = response.results().stream()
                    .map(RetrievalResultItem::score)
                    .max(Double::compareTo)
                    .orElse(0D);
            repository.save(RetrievalAuditLog.builder()
                    .userId(user.userId())
                    .userName(user.userName())
                    .question(response.question())
                    .questionHash(sha256(response.question()))
                    .hitCount(response.results().size())
                    .elapsedMs(response.elapsedMs())
                    .documentIds(String.join(",", documentIds))
                    .maxScore(maxScore)
                    .lowConfidence(response.lowConfidence())
                    .createdTime(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            log.error("Failed to write retrieval audit log, userId={}, question={}",
                    user.userId(), response.question(), ex);
        }
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
