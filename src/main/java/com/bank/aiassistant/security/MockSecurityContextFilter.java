package com.bank.aiassistant.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Mock 认证过滤器。
 *
 * 每个请求进入系统时，如果当前没有认证信息，就写入一个固定管理员用户。
 * 这样 @PreAuthorize、权限过滤和当前用户服务都能基于 SecurityContextHolder 工作。
 */
@Component
public class MockSecurityContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            MockUserPrincipal principal = new MockUserPrincipal(
                    "u1001",
                    "管理员",
                    "tenant_001",
                    List.of("ADMIN", "KNOWLEDGE_MANAGER", "CONFIDENTIAL_ACCESS", "SECRET_ACCESS"),
                    List.of("总行", "科技部")
            );
            List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, "N/A", authorities)
            );
        }
        filterChain.doFilter(request, response);
    }
}
