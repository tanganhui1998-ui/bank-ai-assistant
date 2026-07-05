package com.bank.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全合规增强配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.security.compliance")
public class SecurityComplianceProperties {

    /**
     * 安全审计日志留存天数。银行内部生产环境建议按制度设置为 180 天或更长。
     */
    private int auditRetentionDays = 180;

    /**
     * 单次请假最大天数，超过后进入高风险拦截。
     */
    private int maxLeaveDays = 30;

    /**
     * 是否开启安全响应头。
     */
    private boolean secureHeadersEnabled = true;
}
