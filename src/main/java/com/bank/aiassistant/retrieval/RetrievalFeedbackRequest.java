package com.bank.aiassistant.retrieval;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * RAG 检索反馈请求。
 */
@Getter
@Setter
public class RetrievalFeedbackRequest {

    private String conversationId;

    @NotBlank(message = "question cannot be blank")
    private String question;

    private Boolean helpful;

    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    private String comment;

    private List<String> chunkIds;
}
