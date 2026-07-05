package com.bank.aiassistant.context;

import java.util.List;

public record CurrentUser(
        String userId,
        String userName,
        String tenantId,
        List<String> roles,
        List<String> departments,
        String dataScope,
        String branchNo
) {

    public CurrentUser(String userId, String userName, String tenantId) {
        this(userId, userName, tenantId, List.of(), List.of(), "SELF", "");
    }

    public CurrentUser(String userId, String userName, String tenantId, List<String> roles, List<String> departments) {
        this(userId, userName, tenantId, roles, departments, "SELF", "");
    }
}
