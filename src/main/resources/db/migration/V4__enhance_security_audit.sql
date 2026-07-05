alter table ai_security_audit_log
    add column trace_id varchar(64) null comment '请求追踪ID' after reject_reason,
    add column risk_level varchar(16) null comment '风险等级：LOW/MEDIUM/HIGH' after trace_id,
    add column retention_until datetime null comment '审计日志留存截止时间' after created_time,
    add index idx_security_audit_trace (trace_id),
    add index idx_security_audit_risk_time (risk_level, created_time),
    add index idx_security_audit_retention (retention_until);
