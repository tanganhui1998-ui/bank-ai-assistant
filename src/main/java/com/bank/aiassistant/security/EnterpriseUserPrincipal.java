package com.bank.aiassistant.security;

import java.util.List;

/**
 * 企业真实登录用户主体。
 *
 * 该对象是业务代码读取用户身份的统一载体，来源可以是网关 Header、SSO Token、
 * OAuth2 JWT 或后续接入的统一身份中心。
 */
public record EnterpriseUserPrincipal(
        String userId,
        String userName,
        String tenantId,
        List<String> roles,
        List<String> departments,
        String dataScope,
        String branchNo
) {
}
