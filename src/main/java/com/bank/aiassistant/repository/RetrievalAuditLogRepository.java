package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.RetrievalAuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG 检索审计 MyBatis Mapper。
 */
@Mapper
public interface RetrievalAuditLogRepository {

    @Insert("""
            insert into ai_retrieval_audit_log (
              log_id, user_id, user_name, question, question_hash,
              hit_count, elapsed_ms, document_ids, max_score,
              low_confidence, created_time
            ) values (
              #{logId}, #{userId}, #{userName}, #{question}, #{questionHash},
              #{hitCount}, #{elapsedMs}, #{documentIds}, #{maxScore},
              #{lowConfidence}, #{createdTime}
            )
            """)
    int insert(RetrievalAuditLog log);

    @Select("""
            select question, count(log_id) as query_count, avg(hit_count) as avg_hit_count
            from ai_retrieval_audit_log
            group by question
            order by count(log_id) desc
            limit #{limit}
            """)
    List<Map<String, Object>> findHighFrequencyQueries(int limit);

    @Select("""
            select question, count(log_id) as query_count, avg(hit_count) as avg_hit_count
            from ai_retrieval_audit_log
            where low_confidence = 1
            group by question
            order by count(log_id) desc
            limit #{limit}
            """)
    List<Map<String, Object>> findLowHitQueries(int limit);

    default RetrievalAuditLog save(RetrievalAuditLog log) {
        if (log.getLogId() == null || log.getLogId().isBlank()) {
            log.setLogId(UUID.randomUUID().toString());
        }
        if (log.getCreatedTime() == null) {
            log.setCreatedTime(LocalDateTime.now());
        }
        insert(log);
        return log;
    }
}
