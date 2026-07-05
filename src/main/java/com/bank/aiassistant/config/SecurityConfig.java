package com.bank.aiassistant.config;

import com.bank.aiassistant.security.MockSecurityContextFilter;
import com.bank.aiassistant.security.GatewayHeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置。
 *
 * 当前阶段使用 MockSecurityContextFilter 注入固定用户，所有 HTTP 请求暂时放行；
 * 方法级权限通过 @PreAuthorize 控制发布、废弃等管理动作。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityIdentityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter;
    private final MockSecurityContextFilter mockSecurityContextFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(registry -> registry.anyRequest().authenticated())
                .addFilterBefore(gatewayHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(mockSecurityContextFilter, GatewayHeaderAuthenticationFilter.class)
                .build();
    }
}
