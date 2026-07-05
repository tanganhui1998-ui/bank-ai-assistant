package com.bank.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务系统接入配置。
 *
 * enabled=false 时使用本地 Mock Client，便于无业务系统依赖的开发和测试。
 * enabled=true 时使用 HTTP Client 调用真实请假、薪资、审批、外包系统。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.business-integration")
public class BusinessIntegrationProperties {

    private boolean enabled = false;
    private String baseUrl = "http://localhost:18080";
    private String apiKey = "";
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 5000;

    private String leaveBalancePath = "/api/hr/leave/balance";
    private String leaveRecordsPath = "/api/hr/leave/records";
    private String salaryDetailPath = "/api/hr/salary/detail";
    private String salaryHistoryPath = "/api/hr/salary/history";
    private String approvalProgressPath = "/api/workflow/approval/progress";
    private String todoTasksPath = "/api/workflow/todo/tasks";
    private String outsourcingProgressPath = "/api/outsourcing/application/progress";
    private String executePath = "/api/ai/write/execute";
}
