package com.bank.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.dialog")
public class DialogProperties {

    /**
     * 意图识别 Prompt 模板。使用 {question} 和 {context} 作为占位符。
     */
    private String intentPromptTemplate;

    /**
     * RAG 答案生成 Prompt 模板。使用 {question}、{context}、{citations} 作为占位符。
     */
    private String ragAnswerPromptTemplate;

    /**
     * 闲聊 Prompt 模板。使用 {question} 和 {context} 作为占位符。
     */
    private String chitchatPromptTemplate;

    /**
     * 同一会话内相同问题的意图识别缓存 TTL，单位秒。
     */
    private long intentCacheTtlSeconds = 300L;

    /**
     * 低置信意图识别阈值。
     */
    private double lowConfidenceThreshold = 0.6D;

    /**
     * 加载会话历史的最大条数，避免 Prompt 过长。
     */
    private int maxContextMessages = 6;
}
