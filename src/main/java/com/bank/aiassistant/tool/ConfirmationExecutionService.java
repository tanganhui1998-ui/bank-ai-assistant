package com.bank.aiassistant.tool;

import com.bank.aiassistant.business.BusinessExecutionResult;
import com.bank.aiassistant.business.client.BusinessWriteClient;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.bank.aiassistant.domain.entity.AiBusinessOrder;
import com.bank.aiassistant.domain.entity.PendingOperation;
import com.bank.aiassistant.domain.enums.PendingOperationStatus;
import com.bank.aiassistant.repository.AiBusinessOrderRepository;
import com.bank.aiassistant.repository.PendingOperationRepository;
import com.bank.aiassistant.security.AiSecuritySandboxService;
import com.bank.aiassistant.security.SecurityAuditEvent;
import com.bank.aiassistant.security.SecurityAuditService;
import com.bank.aiassistant.security.SecurityDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 二次确认执行服务。
 *
 * 确认接口是前端唯一能触发 WRITE 操作落地的入口。执行前会重新经过第三层安全校验：
 * Redis 指令存在、创建者一致、当前权限仍有效、业务规则仍满足。
 */
@Service
@RequiredArgsConstructor
public class ConfirmationExecutionService {

    private final CurrentUserProvider currentUserProvider;
    private final PendingConfirmationService pendingConfirmationService;
    private final PendingOperationRepository pendingOperationRepository;
    private final AiBusinessOrderRepository businessOrderRepository;
    private final AiSecuritySandboxService sandboxService;
    private final SecurityAuditService securityAuditService;
    private final BusinessWriteClient businessWriteClient;

    @Transactional
    public ConfirmationExecutionResult confirm(String pendingId) {
        long start = System.currentTimeMillis();
        CurrentUser user = currentUserProvider.currentUser();
        PendingConfirmationRecord record = pendingConfirmationService.get(user.userId(), pendingId);
        SecurityDecision decision = sandboxService.checkConfirm(user, record);
        if (!decision.allowed()) {
            ConfirmationExecutionResult denied = ConfirmationExecutionResult.builder()
                    .success(false)
                    .message(decision.reason())
                    .data(Map.of("pendingId", pendingId))
                    .build();
            audit(user, "CONFIRM_REJECTED", record, denied, true, decision.reason(), start);
            return denied;
        }

        PendingOperation operation = pendingOperationRepository.findById(pendingId).orElse(null);
        if (operation != null) {
            operation.setStatus(PendingOperationStatus.CONFIRMED);
            operation.setConfirmedTime(LocalDateTime.now());
            pendingOperationRepository.save(operation);
        }
        pendingConfirmationService.remove(user.userId(), pendingId);
        ConfirmationExecutionResult result = executeBusinessApi(user, record);
        audit(user, "CONFIRM_EXECUTED", record, result, false, null, start);
        return result;
    }

    @Transactional
    public ConfirmationExecutionResult cancel(String pendingId) {
        long start = System.currentTimeMillis();
        CurrentUser user = currentUserProvider.currentUser();
        PendingConfirmationRecord record = pendingConfirmationService.get(user.userId(), pendingId);
        PendingOperation operation = pendingOperationRepository.findById(pendingId).orElse(null);
        if (operation != null) {
            operation.setStatus(PendingOperationStatus.CANCELED);
            pendingOperationRepository.save(operation);
        }
        pendingConfirmationService.remove(user.userId(), pendingId);
        ConfirmationExecutionResult result = ConfirmationExecutionResult.builder()
                .success(true)
                .canceled(true)
                .message("已取消本次操作。")
                .data(Map.of("pendingId", pendingId))
                .build();
        audit(user, "CONFIRM_CANCELED", record, result, false, null, start);
        return result;
    }

    private ConfirmationExecutionResult executeBusinessApi(CurrentUser user, PendingConfirmationRecord record) {
        BusinessExecutionResult executionResult = businessWriteClient.execute(user, record.toolName(), record.params());
        String businessOrderNo = executionResult.businessOrderNo() == null || executionResult.businessOrderNo().isBlank()
                ? record.pendingId()
                : executionResult.businessOrderNo();
        businessOrderRepository.save(AiBusinessOrder.builder()
                .pendingId(record.pendingId())
                .businessOrderNo(businessOrderNo)
                .userId(user.userId())
                .toolName(record.toolName())
                .status(executionResult.success() ? "SUCCESS" : "FAILED")
                .createdTime(LocalDateTime.now())
                .build());
        return ConfirmationExecutionResult.builder()
                .success(executionResult.success())
                .message(executionResult.message())
                .data(Map.of(
                        "pendingId", record.pendingId(),
                        "businessOrderNo", businessOrderNo,
                        "toolName", record.toolName(),
                        "businessResult", executionResult.data() == null ? Map.of() : executionResult.data()
                ))
                .build();
    }

    private void audit(
            CurrentUser user,
            String actionType,
            PendingConfirmationRecord record,
            ConfirmationExecutionResult result,
            boolean rejected,
            String rejectReason,
            long start
    ) {
        securityAuditService.record(SecurityAuditEvent.builder()
                .user(user)
                .actionType(actionType)
                .toolName(record == null ? null : record.toolName())
                .operationType("WRITE_CONFIRM")
                .inputParams(record == null ? Map.of() : record.params())
                .result(result)
                .rejected(rejected)
                .rejectReason(rejectReason)
                .elapsedMs(System.currentTimeMillis() - start)
                .build());
    }
}
