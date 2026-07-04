package com.bank.aiassistant.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QwenProperties.class)
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel qwenChatLanguageModel(QwenProperties properties) {
        return OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getChat().getModel())
                .temperature(properties.getChat().getTemperature())
                .build();
    }

    @Bean
    public StreamingChatLanguageModel qwenStreamingChatLanguageModel(QwenProperties properties) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getChat().getModel())
                .temperature(properties.getChat().getTemperature())
                .build();
    }

    @Bean
    public EmbeddingModel qwenEmbeddingModel(QwenProperties properties) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getEmbedding().getModel())
                .build();
    }
}
