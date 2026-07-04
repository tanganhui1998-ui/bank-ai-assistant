package com.bank.aiassistant.tool;

import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.domain.entity.AiToolCallAudit;
import com.bank.aiassistant.repository.AiToolCallAuditRepository;
import com.bank.aiassistant.security.SensitiveDataMasker;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工具调用审计异步写入服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallAuditService {

    private final AiToolCallAuditRepository repository;
    private final ObjectMapper objectMapper;
    private final SensitiveDataMasker sensitiveDataMasker;

    @Async("retrievalExecutor")
    public void record(CurrentUser user, String toolName, Map<String, Object> params, ToolCallResult result, long elapsedMs) {
        try {
            repository.save(AiToolCallAudit.builder()
                    .userId(user.userId())
                    .toolName(toolName)
                    .inputParamsJson(objectMapper.writeValueAsString(sensitiveDataMasker.mask(params)))
                    .callResult(objectMapper.writeValueAsString(result))
                    .permissionRejected(result.permissionRejected())
                    .rejectReason(result.permissionRejected() ? result.message() : null)
                    .calledTime(LocalDateTime.now())
                    .elapsedMs(elapsedMs)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to write tool audit log, userId={}, toolName={}", user.userId(), toolName, ex);
        }
    }
}
