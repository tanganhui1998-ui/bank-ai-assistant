package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.PendingOperation;
import com.bank.aiassistant.domain.enums.PendingOperationStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 待确认操作 MyBatis Mapper。
 */
@Mapper
public interface PendingOperationRepository {

    String BASE_COLUMNS = """
            pending_id, user_id, operation_type, tool_name, business_params_json,
            operation_summary, status, expire_time, created_time, confirmed_time
            """;

    @Select("select " + BASE_COLUMNS + " from ai_pending_operation where pending_id = #{pendingId}")
    Optional<PendingOperation> findById(String pendingId);

    @Select("select " + BASE_COLUMNS + " from ai_pending_operation order by created_time desc")
    List<PendingOperation> findAll();

    @Select("""
            select """ + BASE_COLUMNS + """
            from ai_pending_operation
            where user_id = #{userId}
              and status = #{status}
            order by created_time desc
            """)
    List<PendingOperation> findByUserIdAndStatusOrderByCreatedTimeDesc(
            @Param("userId") String userId,
            @Param("status") PendingOperationStatus status
    );

    @Select("""
            select """ + BASE_COLUMNS + """
            from ai_pending_operation
            where status = #{status}
              and expire_time < #{expireTime}
            order by expire_time asc
            """)
    List<PendingOperation> findByStatusAndExpireTimeBefore(
            @Param("status") PendingOperationStatus status,
            @Param("expireTime") LocalDateTime expireTime
    );

    @Insert("""
            insert into ai_pending_operation (
              pending_id, user_id, operation_type, tool_name, business_params_json,
              operation_summary, status, expire_time, created_time, confirmed_time
            ) values (
              #{pendingId}, #{userId}, #{operationType}, #{toolName}, #{businessParamsJson},
              #{operationSummary}, #{status}, #{expireTime}, #{createdTime}, #{confirmedTime}
            )
            """)
    int insert(PendingOperation operation);

    @Update("""
            update ai_pending_operation
            set user_id = #{userId},
                operation_type = #{operationType},
                tool_name = #{toolName},
                business_params_json = #{businessParamsJson},
                operation_summary = #{operationSummary},
                status = #{status},
                expire_time = #{expireTime},
                created_time = #{createdTime},
                confirmed_time = #{confirmedTime}
            where pending_id = #{pendingId}
            """)
    int update(PendingOperation operation);

    default PendingOperation save(PendingOperation operation) {
        if (operation.getPendingId() == null || operation.getPendingId().isBlank()) {
            operation.setPendingId(UUID.randomUUID().toString());
        }
        if (operation.getCreatedTime() == null) {
            operation.setCreatedTime(LocalDateTime.now());
        }
        if (findById(operation.getPendingId()).isPresent()) {
            update(operation);
        } else {
            insert(operation);
        }
        return operation;
    }
}
