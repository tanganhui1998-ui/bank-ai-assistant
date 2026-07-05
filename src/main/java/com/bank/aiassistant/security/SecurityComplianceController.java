package com.bank.aiassistant.security;

import com.bank.aiassistant.web.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 安全合规管理接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/security/compliance")
public class SecurityComplianceController {

    private final SecurityAuditQueryService auditQueryService;
    private final SecurityReconciliationService reconciliationService;

    @GetMapping("/audits/by-user")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_AUDITOR')")
    public Result<List<SecurityAuditResponse>> auditsByUser(
            @RequestParam String userId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return Result.success(auditQueryService.recentByUser(userId, limit));
    }

    @GetMapping("/audits/by-risk")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_AUDITOR')")
    public Result<List<SecurityAuditResponse>> auditsByRisk(
            @RequestParam(defaultValue = "HIGH") String riskLevel,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return Result.success(auditQueryService.recentByRiskLevel(riskLevel, limit));
    }

    @PostMapping("/reconcile")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_AUDITOR')")
    public Result<SecurityReconciliationReport> reconcile() {
        return Result.success(reconciliationService.reconcile());
    }
}
