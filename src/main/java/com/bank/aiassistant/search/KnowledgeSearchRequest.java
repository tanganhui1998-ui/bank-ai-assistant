package com.bank.aiassistant.search;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 知识库检索请求。
 */
@Getter
@Setter
public class KnowledgeSearchRequest {

    @NotBlank(message = "query cannot be blank")
    private String query;

    private int size = 10;
}
