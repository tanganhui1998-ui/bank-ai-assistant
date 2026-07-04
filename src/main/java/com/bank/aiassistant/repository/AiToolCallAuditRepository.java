package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.AiToolCallAudit;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 工具调用审计 MyBatis Mapper。
 */
@Mapper
public interface AiToolCallAuditRepository {

    String BASE_COLUMNS = """
            call_id, user_id, tool_name, input_params_json, call_result,
            permission_rejected, reject_reason, called_time, elapsed_ms
            """;

    @Select("select " + BASE_COLUMNS + " from ai_tool_call_audit where user_id = #{userId} order by called_time desc")
    List<AiToolCallAudit> findByUserIdOrderByCalledTimeDesc(String userId);

    @Select("select " + BASE_COLUMNS + " from ai_tool_call_audit where tool_name = #{toolName} order by called_time desc")
    List<AiToolCallAudit> findByToolNameOrderByCalledTimeDesc(String toolName);

    @Select("select " + BASE_COLUMNS + " from ai_tool_call_audit where permission_rejected = 1 order by called_time desc")
    List<AiToolCallAudit> findByPermissionRejectedTrueOrderByCalledTimeDesc();

    @Insert("""
            insert into ai_tool_call_audit (
              call_id, user_id, tool_name, input_params_json, call_result,
              permission_rejected, reject_reason, called_time, elapsed_ms
            ) values (
              #{callId}, #{userId}, #{toolName}, #{inputParamsJson}, #{callResult},
              #{permissionRejected}, #{rejectReason}, #{calledTime}, #{elapsedMs}
            )
            """)
    int insert(AiToolCallAudit audit);

    default AiToolCallAudit save(AiToolCallAudit audit) {
        if (audit.getCallId() == null || audit.getCallId().isBlank()) {
            audit.setCallId(UUID.randomUUID().toString());
        }
        if (audit.getCalledTime() == null) {
            audit.setCalledTime(LocalDateTime.now());
        }
        insert(audit);
        return audit;
    }
}
