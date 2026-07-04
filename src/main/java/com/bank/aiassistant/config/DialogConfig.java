package com.bank.aiassistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 对话入口配置启用类。
 */
@Configuration
@EnableConfigurationProperties(DialogProperties.class)
public class DialogConfig {
}
