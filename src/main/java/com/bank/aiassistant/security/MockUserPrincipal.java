package com.bank.aiassistant.security;

import java.util.List;

/**
 * Mock 登录用户主体。
 *
 * 当前项目还没有接入真实用户中心，因此先把用户 ID、姓名、租户、角色和部门
 * 放在 SecurityContextHolder 的 principal 中。后续接入 SSO/OAuth2 时，只需要
 * 替换填充 principal 的认证过滤器，业务层读取方式可以保持不变。
 */
public record MockUserPrincipal(
        String userId,
        String userName,
        String tenantId,
        List<String> roles,
        List<String> departments,
        String dataScope,
        String branchNo
) {

    public MockUserPrincipal(String userId, String userName, String tenantId, List<String> roles, List<String> departments) {
        this(userId, userName, tenantId, roles, departments, "ALL", "0000");
    }
}
