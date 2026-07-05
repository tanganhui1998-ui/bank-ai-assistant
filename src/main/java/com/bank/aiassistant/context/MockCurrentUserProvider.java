package com.bank.aiassistant.context;

import com.bank.aiassistant.security.MockUserPrincipal;
import com.bank.aiassistant.security.EnterpriseUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 当前用户上下文实现。
 *
 * 业务代码统一依赖 CurrentUserProvider，不直接依赖认证来源。
 * Header、SSO、JWT 和本地 Mock 最终都会被转换为 CurrentUser。
 */
@Component
public class MockCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof EnterpriseUserPrincipal principal) {
            return new CurrentUser(
                    principal.userId(),
                    principal.userName(),
                    principal.tenantId(),
                    principal.roles(),
                    principal.departments(),
                    principal.dataScope(),
                    principal.branchNo()
            );
        }
        if (authentication != null && authentication.getPrincipal() instanceof MockUserPrincipal principal) {
            return new CurrentUser(
                    principal.userId(),
                    principal.userName(),
                    principal.tenantId(),
                    principal.roles(),
                    principal.departments()
            );
        }
        return new CurrentUser("u1001", "管理员", "tenant_001", List.of("ADMIN"), List.of("总行"));
    }
}
