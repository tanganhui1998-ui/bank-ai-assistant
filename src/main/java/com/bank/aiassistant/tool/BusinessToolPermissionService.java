package com.bank.aiassistant.tool;

import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 业务工具权限预检服务。
 *
 * 当前阶段使用 Mock 角色判断；后续接入真实权限中心后，只需要替换这里的规则。
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
        boolean admin = hasRole(user, "ADMIN");
        boolean manager = hasRole(user, "KNOWLEDGE_MANAGER");
        boolean selfOperation = targetUserId == null || targetUserId.isBlank() || user.userId().equals(targetUserId);

        if (operationType == ToolOperationType.READ && (selfOperation || admin || manager)) {
            return null;
        }
        if (operationType == ToolOperationType.WRITE && (selfOperation || admin)) {
            return null;
        }
        return ToolCallResult.permissionDenied("您没有权限执行工具：" + toolName + "。如需办理，请联系管理员或相关部门。");
    }

    private boolean hasRole(CurrentUser user, String role) {
        return user.roles() != null && user.roles().contains(role);
    }
}
