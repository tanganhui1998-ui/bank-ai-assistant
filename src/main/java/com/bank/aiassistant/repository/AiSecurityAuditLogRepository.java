package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.AiSecurityAuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 安全审计日志 MyBatis Mapper。
 */
@Mapper
public interface AiSecurityAuditLogRepository {

    @Insert("""
            insert into ai_security_audit_log (
              audit_id, user_id, user_name, session_id, action_type, tool_name,
              operation_type, input_params_json, result_json, rejected,
              reject_reason, trace_id, risk_level, elapsed_ms, client_ip, created_time, retention_until
            ) values (
              #{auditId}, #{userId}, #{userName}, #{sessionId}, #{actionType}, #{toolName},
              #{operationType}, #{inputParamsJson}, #{resultJson}, #{rejected},
              #{rejectReason}, #{traceId}, #{riskLevel}, #{elapsedMs}, #{clientIp}, #{createdTime}, #{retentionUntil}
            )
            """)
    int insert(AiSecurityAuditLog log);

    @Select("""
            select audit_id, user_id, user_name, session_id, action_type, tool_name,
                   operation_type, input_params_json, result_json, rejected,
                   reject_reason, trace_id, risk_level, elapsed_ms, client_ip, created_time, retention_until
            from ai_security_audit_log
            where user_id = #{userId}
            order by created_time desc
            limit #{limit}
            """)
    List<AiSecurityAuditLog> findRecentByUserId(String userId, int limit);

    @Select("""
            select audit_id, user_id, user_name, session_id, action_type, tool_name,
                   operation_type, input_params_json, result_json, rejected,
                   reject_reason, trace_id, risk_level, elapsed_ms, client_ip, created_time, retention_until
            from ai_security_audit_log
            where risk_level = #{riskLevel}
            order by created_time desc
            limit #{limit}
            """)
    List<AiSecurityAuditLog> findRecentByRiskLevel(String riskLevel, int limit);

    default AiSecurityAuditLog save(AiSecurityAuditLog log) {
        if (log.getAuditId() == null || log.getAuditId().isBlank()) {
            log.setAuditId(UUID.randomUUID().toString());
        }
        if (log.getCreatedTime() == null) {
            log.setCreatedTime(LocalDateTime.now());
        }
        insert(log);
        return log;
    }
}
