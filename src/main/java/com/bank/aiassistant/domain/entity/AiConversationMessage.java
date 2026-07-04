package com.bank.aiassistant.domain.entity;

import com.bank.aiassistant.domain.enums.AiSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 会话消息记录。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationMessage {

    private String messageId;
    private String sessionId;
    private String parentMessageId;
    private String userId;
    private String userQuestion;
    private String aiAnswer;
    private String referencesJson;
    private String toolCallsJson;
    private AiSessionStatus sessionStatus;
    private LocalDateTime createdTime;
    private Long elapsedMs;
}
