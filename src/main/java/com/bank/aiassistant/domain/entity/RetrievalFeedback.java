package com.bank.aiassistant.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * RAG 检索反馈记录。
 *
 * 该表用于沉淀用户对检索结果和回答质量的显式反馈，
 * 后续可作为评测集扩充、同义词维护和重排参数优化的数据来源。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalFeedback {

    private String feedbackId;
    private String userId;
    private String userName;
    private String conversationId;
    private String question;
    private Boolean helpful;
    private Integer rating;
    private String comment;
    private String chunkIds;
    private LocalDateTime createdTime;
}
