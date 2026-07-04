package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.AiConversationMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 会话消息 MyBatis Mapper。
 */
@Mapper
public interface AiConversationMessageRepository {

    String BASE_COLUMNS = """
            message_id, session_id, parent_message_id, user_id, user_question,
            ai_answer, references_json, tool_calls_json, session_status,
            created_time, elapsed_ms
            """;

    @Select("select " + BASE_COLUMNS + " from ai_conversation_message where session_id = #{sessionId} order by created_time asc")
    List<AiConversationMessage> findBySessionIdOrderByCreatedTimeAsc(String sessionId);

    @Select("select " + BASE_COLUMNS + " from ai_conversation_message where user_id = #{userId} order by created_time desc")
    List<AiConversationMessage> findByUserIdOrderByCreatedTimeDesc(String userId);

    @Select("select " + BASE_COLUMNS + " from ai_conversation_message where parent_message_id = #{parentMessageId} order by created_time asc")
    List<AiConversationMessage> findByParentMessageIdOrderByCreatedTimeAsc(String parentMessageId);

    @Insert("""
            insert into ai_conversation_message (
              message_id, session_id, parent_message_id, user_id, user_question,
              ai_answer, references_json, tool_calls_json, session_status,
              created_time, elapsed_ms
            ) values (
              #{messageId}, #{sessionId}, #{parentMessageId}, #{userId}, #{userQuestion},
              #{aiAnswer}, #{referencesJson}, #{toolCallsJson}, #{sessionStatus},
              #{createdTime}, #{elapsedMs}
            )
            """)
    int insert(AiConversationMessage message);

    default AiConversationMessage save(AiConversationMessage message) {
        if (message.getMessageId() == null || message.getMessageId().isBlank()) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        if (message.getCreatedTime() == null) {
            message.setCreatedTime(LocalDateTime.now());
        }
        insert(message);
        return message;
    }
}
