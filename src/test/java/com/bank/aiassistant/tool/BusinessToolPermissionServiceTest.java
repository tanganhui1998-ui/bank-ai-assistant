package com.bank.aiassistant.tool;

import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessToolPermissionServiceTest {

    @Test
    void salaryQueryShouldAllowSelfUser() {
        BusinessToolPermissionService service = new BusinessToolPermissionService(provider(new CurrentUser(
                "u1001", "员工", "tenant_001", List.of("EMPLOYEE"), List.of("科技部"), "SELF", "0101"
        )));

        assertThat(service.check("query_salary_detail", ToolOperationType.READ, "u1001")).isNull();
    }

    @Test
    void salaryQueryShouldRejectOtherUserWithoutHrScope() {
        BusinessToolPermissionService service = new BusinessToolPermissionService(provider(new CurrentUser(
                "u1001", "员工", "tenant_001", List.of("EMPLOYEE"), List.of("科技部"), "SELF", "0101"
        )));

        ToolCallResult result = service.check("query_salary_detail", ToolOperationType.READ, "u2001");

        assertThat(result).isNotNull();
        assertThat(result.permissionRejected()).isTrue();
    }

    @Test
    void approvalWriteShouldAllowApproverRole() {
        BusinessToolPermissionService service = new BusinessToolPermissionService(provider(new CurrentUser(
                "u1001", "审批人", "tenant_001", List.of("APPROVER"), List.of("科技部"), "DEPARTMENT", "0101"
        )));

        assertThat(service.check("submit_approval_opinion", ToolOperationType.WRITE, "u1001")).isNull();
    }

    private CurrentUserProvider provider(CurrentUser user) {
        return () -> user;
    }
}
