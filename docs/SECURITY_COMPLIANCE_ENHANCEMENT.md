# 阶段六：安全合规增强

## 本阶段新增能力

1. 请求追踪：新增 `X-Trace-Id`，写入响应头、MDC 和安全审计表。
2. 审计增强：`ai_security_audit_log` 新增 `trace_id`、`risk_level`、`retention_until`。
3. 结果脱敏：审计入参和执行结果都会脱敏，支持 Map/List 递归处理，并识别手机号、身份证、银行卡文本。
4. 风险拦截：二次确认后、真实业务执行前增加风险规则，例如超长请假和高风险审批意见。
5. 合规查询：新增 `/api/security/compliance/audits/by-user`、`/audits/by-risk`、`/reconcile`。
6. 安全响应头：启用 CSP、Frame-Options 等基础响应头，减少点击劫持和内容嗅探风险。

## 生产配置建议

```yaml
app:
  ai:
    security:
      compliance:
        audit-retention-days: 180
        max-leave-days: 30
  security:
    identity:
      mock-fallback-enabled: false
```

## 后续建议

1. 将 `SecurityRiskAssessmentService` 替换为统一风控中心或规则引擎。
2. 将审计日志同步到不可篡改存储或 SIEM。
3. 针对高风险事件增加短信、企业微信或工单告警。
4. 为合规接口增加导出审批流，避免审计数据被滥查。
