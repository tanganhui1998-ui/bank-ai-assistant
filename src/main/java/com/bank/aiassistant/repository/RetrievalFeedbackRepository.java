package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.RetrievalFeedback;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RAG 检索反馈 MyBatis Mapper。
 */
@Mapper
public interface RetrievalFeedbackRepository {

    @Insert("""
            insert into ai_retrieval_feedback (
              feedback_id, user_id, user_name, conversation_id, question,
              helpful, rating, comment, chunk_ids, created_time
            ) values (
              #{feedbackId}, #{userId}, #{userName}, #{conversationId}, #{question},
              #{helpful}, #{rating}, #{comment}, #{chunkIds}, #{createdTime}
            )
            """)
    int insert(RetrievalFeedback feedback);

    default RetrievalFeedback save(RetrievalFeedback feedback) {
        if (feedback.getFeedbackId() == null || feedback.getFeedbackId().isBlank()) {
            feedback.setFeedbackId(UUID.randomUUID().toString());
        }
        if (feedback.getCreatedTime() == null) {
            feedback.setCreatedTime(LocalDateTime.now());
        }
        insert(feedback);
        return feedback;
    }
}
