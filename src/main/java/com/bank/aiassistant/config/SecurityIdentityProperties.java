package com.bank.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 真实身份接入配置。
 *
 * 生产环境通常由 API 网关、SSO 或 OAuth2 Resource Server 完成认证，
 * 本服务只信任内网网关透传的身份 Header，并统一写入 SecurityContextHolder。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.identity")
public class SecurityIdentityProperties {

    /**
     * 是否启用 Header 身份注入。
     */
    private boolean headerAuthEnabled = true;

    /**
     * 缺少真实身份 Header 时是否回退到本地 Mock 管理员。
     * 生产环境必须设置为 false，避免绕过真实认证。
     */
    private boolean mockFallbackEnabled = true;

    private String userIdHeader = "X-User-Id";
    private String userNameHeader = "X-User-Name";
    private String tenantIdHeader = "X-Tenant-Id";
    private String rolesHeader = "X-User-Roles";
    private String departmentsHeader = "X-User-Departments";
    private String dataScopeHeader = "X-Data-Scope";
    private String branchNoHeader = "X-Branch-No";
}
