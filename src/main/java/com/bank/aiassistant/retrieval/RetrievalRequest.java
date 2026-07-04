package com.bank.aiassistant.retrieval;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * AI 助手在线检索请求。
 */
@Getter
@Setter
public class RetrievalRequest {

    @NotBlank(message = "question cannot be blank")
    private String question;

    private Integer topK = 5;

    @Valid
    private RetrievalFilters filters;
}
