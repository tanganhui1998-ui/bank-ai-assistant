package com.bank.aiassistant.security;

import com.bank.aiassistant.config.SecurityIdentityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 网关 Header 身份认证过滤器。
 *
 * 生产接入建议：
 * 1. 外部请求先经过统一身份认证网关。
 * 2. 网关校验 Token 后向内网服务透传 X-User-* Header。
 * 3. 本过滤器只消费可信内网 Header，不直接解析用户密码或登录态。
 */
@Component
@RequiredArgsConstructor
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityIdentityProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null && properties.isHeaderAuthEnabled()) {
            String userId = header(request, properties.getUserIdHeader());
            if (StringUtils.hasText(userId)) {
                EnterpriseUserPrincipal principal = new EnterpriseUserPrincipal(
                        userId,
                        defaultValue(decode(header(request, properties.getUserNameHeader())), userId),
                        defaultValue(header(request, properties.getTenantIdHeader()), "tenant_001"),
                        split(header(request, properties.getRolesHeader())),
                        split(header(request, properties.getDepartmentsHeader())),
                        defaultValue(header(request, properties.getDataScopeHeader()), "SELF"),
                        defaultValue(header(request, properties.getBranchNoHeader()), "")
                );
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, "N/A", authorities(principal.roles()))
                );
            }
        }
        filterChain.doFilter(request, response);
    }

    private String header(HttpServletRequest request, String name) {
        return StringUtils.hasText(name) ? request.getHeader(name) : null;
    }

    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private List<SimpleGrantedAuthority> authorities(List<String> roles) {
        return roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private String decode(String value) {
        return value == null ? null : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String defaultValue(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
