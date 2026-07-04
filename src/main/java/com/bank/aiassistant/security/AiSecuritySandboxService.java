package com.bank.aiassistant.security;

import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.dialog.IntentRecognitionResult;
import com.bank.aiassistant.dialog.IntentType;
import com.bank.aiassistant.tool.BusinessToolPermissionService;
import com.bank.aiassistant.tool.PendingConfirmationRecord;
import com.bank.aiassistant.tool.ToolCallResult;
import com.bank.aiassistant.tool.ToolOperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI 助手安全沙箱。
 *
 * 三层防线：
 * 1. 意图识别后做权限预检，禁止无权限请求进入检索/工具链路。
 * 2. WRITE 工具只生成待确认指令，不直接执行。
 * 3. 用户确认后再次校验 Redis 指令归属、当前权限和业务规则。
 */
@Service
@RequiredArgsConstructor
public class AiSecuritySandboxService {

    private final BusinessToolPermissionService businessToolPermissionService;

    public SecurityDecision checkIntent(CurrentUser user, IntentRecognitionResult intent) {
        if (intent == null || intent.intent() == null) {
            return SecurityDecision.deny("无法识别请求意图。");
        }
        if (intent.intent() == IntentType.BUSINESS_EXECUTE && !hasAnyRole(user, "ADMIN", "EMPLOYEE", "KNOWLEDGE_MANAGER")) {
            return SecurityDecision.deny("您没有权限执行此操作。");
        }
        if (intent.intent() == IntentType.BUSINESS_QUERY && user.userId() == null) {
            return SecurityDecision.deny("请先登录后再查询业务数据。");
        }
        return SecurityDecision.allow();
    }

    public SecurityDecision checkConfirm(CurrentUser user, PendingConfirmationRecord record) {
        if (record == null) {
            return SecurityDecision.deny("待确认指令不存在或已超时。");
        }
        if (!user.userId().equals(record.userId())) {
            return SecurityDecision.deny("当前用户不是该指令的创建者。");
        }
        if (record.expireTime() != null && record.expireTime().isBefore(LocalDateTime.now())) {
            return SecurityDecision.deny("待确认指令已过期。");
        }
        ToolCallResult permission = businessToolPermissionService.check(record.toolName(), ToolOperationType.WRITE, user.userId());
        if (permission != null && permission.permissionRejected()) {
            return SecurityDecision.deny(permission.message());
        }
        return validateBusinessRules(record);
    }

    private SecurityDecision validateBusinessRules(PendingConfirmationRecord record) {
        if ("create_leave_application".equals(record.toolName())) {
            if (isBlank(record.params().get("startTime")) || isBlank(record.params().get("endTime"))) {
                return SecurityDecision.deny("请假开始时间和结束时间不能为空。");
            }
            if (isBlank(record.params().get("reason"))) {
                return SecurityDecision.deny("请假原因不能为空。");
            }
        }
        if ("submit_approval_opinion".equals(record.toolName()) && isBlank(record.params().get("decision"))) {
            return SecurityDecision.deny("审批结论不能为空。");
        }
        return SecurityDecision.allow();
    }

    private boolean hasAnyRole(CurrentUser user, String... roles) {
        if (user.roles() == null) {
            return false;
        }
        for (String role : roles) {
            if (user.roles().contains(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }
}
