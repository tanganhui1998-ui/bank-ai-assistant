package com.bank.aiassistant.tool;

import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 业务工具权限预检服务。
 *
 * 当前实现基于真实用户上下文中的角色和数据范围判断。
 * 后续接入权限中心时，可以把 hasRole/hasDataScope 替换为远程鉴权结果。
 */
@Service
@RequiredArgsConstructor
public class BusinessToolPermissionService {

    private final CurrentUserProvider currentUserProvider;

    public CurrentUser currentUser() {
        return currentUserProvider.currentUser();
    }

    public ToolCallResult check(String toolName, ToolOperationType operationType, String targetUserId) {
        CurrentUser user = currentUser();
        boolean selfOperation = targetUserId == null || targetUserId.isBlank() || user.userId().equals(targetUserId);

        if (operationType == ToolOperationType.READ && canRead(user, toolName, selfOperation)) {
            return null;
        }
        if (operationType == ToolOperationType.WRITE && canWrite(user, toolName, selfOperation)) {
            return null;
        }
        return ToolCallResult.permissionDenied("您没有权限执行工具：" + toolName + "。如需办理，请联系管理员或相关部门。");
    }

    private boolean canRead(CurrentUser user, String toolName, boolean selfOperation) {
        if (hasRole(user, "ADMIN")) {
            return true;
        }
        if (toolName.startsWith("query_salary")) {
            return selfOperation || hasRole(user, "HR") && hasDataScope(user, "DEPARTMENT", "ALL");
        }
        if ("query_todo_tasks".equals(toolName)) {
            return selfOperation || hasRole(user, "APPROVAL_MANAGER");
        }
        if (toolName.startsWith("query_outsourcing")) {
            return selfOperation || hasRole(user, "OUTSOURCING_MANAGER");
        }
        return selfOperation || hasRole(user, "HR") || hasRole(user, "APPROVAL_MANAGER");
    }

    private boolean canWrite(CurrentUser user, String toolName, boolean selfOperation) {
        if (hasRole(user, "ADMIN")) {
            return true;
        }
        if ("create_leave_application".equals(toolName)) {
            return selfOperation || hasRole(user, "HR");
        }
        if ("submit_approval_opinion".equals(toolName)) {
            return hasRole(user, "APPROVER") || hasRole(user, "APPROVAL_MANAGER");
        }
        if ("create_outsourcing_entry_application".equals(toolName)) {
            return hasRole(user, "OUTSOURCING_MANAGER") || hasRole(user, "PROJECT_MANAGER");
        }
        return false;
    }

    private boolean hasRole(CurrentUser user, String role) {
        return user.roles() != null && user.roles().contains(role);
    }

    private boolean hasDataScope(CurrentUser user, String... scopes) {
        if (user.dataScope() == null) {
            return false;
        }
        for (String scope : scopes) {
            if (user.dataScope().equals(scope)) {
                return true;
            }
        }
        return false;
    }
}
