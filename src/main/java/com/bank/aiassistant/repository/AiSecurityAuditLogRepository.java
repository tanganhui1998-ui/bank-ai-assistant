package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.AiSecurityAuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
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
              reject_reason, elapsed_ms, client_ip, created_time
            ) values (
              #{auditId}, #{userId}, #{userName}, #{sessionId}, #{actionType}, #{toolName},
              #{operationType}, #{inputParamsJson}, #{resultJson}, #{rejected},
              #{rejectReason}, #{elapsedMs}, #{clientIp}, #{createdTime}
            )
            """)
    int insert(AiSecurityAuditLog log);

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
