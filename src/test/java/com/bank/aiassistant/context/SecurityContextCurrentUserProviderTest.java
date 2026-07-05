package com.bank.aiassistant.context;

import com.bank.aiassistant.security.EnterpriseUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextCurrentUserProviderTest {

    private final MockCurrentUserProvider provider = new MockCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserShouldReadEnterprisePrincipalFromSecurityContext() {
        EnterpriseUserPrincipal principal = new EnterpriseUserPrincipal(
                "u2001",
                "张三",
                "tenant_real",
                List.of("EMPLOYEE", "APPROVER"),
                List.of("科技部"),
                "DEPARTMENT",
                "0101"
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "N/A", List.of())
        );

        CurrentUser user = provider.currentUser();

        assertThat(user.userId()).isEqualTo("u2001");
        assertThat(user.roles()).contains("APPROVER");
        assertThat(user.dataScope()).isEqualTo("DEPARTMENT");
        assertThat(user.branchNo()).isEqualTo("0101");
    }
}
