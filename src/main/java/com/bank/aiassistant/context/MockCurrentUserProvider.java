package com.bank.aiassistant.context;

import com.bank.aiassistant.security.MockUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 当前用户上下文 Mock 实现。
 *
 * 业务代码统一依赖 CurrentUserProvider，不直接依赖 Mock。现在从
 * SecurityContextHolder 读取用户，未来接入真实用户体系时替换认证过滤器即可。
 */
@Component
public class MockCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
