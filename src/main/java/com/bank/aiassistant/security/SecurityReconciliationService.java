package com.bank.aiassistant.security;

import com.bank.aiassistant.domain.entity.PendingOperation;
import com.bank.aiassistant.domain.enums.PendingOperationStatus;
import com.bank.aiassistant.repository.AiBusinessOrderRepository;
import com.bank.aiassistant.repository.PendingOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 零误操作对账服务。
 *
 * 定期检查 AI 确认执行记录与业务订单记录是否一致，并把超时未确认指令转为 EXPIRED。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityReconciliationService {

    private final PendingOperationRepository pendingOperationRepository;
    private final AiBusinessOrderRepository businessOrderRepository;

    @Scheduled(fixedDelayString = "${app.ai.security.reconcile-fixed-delay-ms:300000}")
    @Transactional
    public SecurityReconciliationReport reconcile() {
        List<String> abnormalPendingIds = new ArrayList<>();
        long confirmedWithoutOrderCount = 0;
        long expiredFixedCount = 0;

        for (PendingOperation operation : pendingOperationRepository.findAll()) {
            if (operation.getStatus() == PendingOperationStatus.CONFIRMED
                    && !businessOrderRepository.existsByPendingId(operation.getPendingId())) {
                confirmedWithoutOrderCount++;
                abnormalPendingIds.add(operation.getPendingId());
            }
            if (operation.getStatus() == PendingOperationStatus.PENDING_CONFIRM
                    && operation.getExpireTime().isBefore(LocalDateTime.now())) {
                operation.setStatus(PendingOperationStatus.EXPIRED);
                pendingOperationRepository.save(operation);
                expiredFixedCount++;
            }
        }

        SecurityReconciliationReport report = SecurityReconciliationReport.builder()
                .generatedTime(LocalDateTime.now())
                .confirmedWithoutOrderCount(confirmedWithoutOrderCount)
                .expiredFixedCount(expiredFixedCount)
                .abnormalPendingIds(abnormalPendingIds)
                .build();
        log.info("AI security reconciliation report: {}", report);
        return report;
    }
}
