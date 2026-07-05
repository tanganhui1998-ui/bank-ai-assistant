package com.bank.aiassistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 业务系统集成配置入口。
 */
@Configuration
@EnableConfigurationProperties(BusinessIntegrationProperties.class)
public class BusinessIntegrationConfig {
}
