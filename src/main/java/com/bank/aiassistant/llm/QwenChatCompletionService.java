package com.bank.aiassistant.llm;

import com.bank.aiassistant.config.QwenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 通义千问 Chat Completion 调用服务。
 *
 * 使用 OpenAI-compatible 接口，供意图识别、RAG答案生成、闲聊等链路复用。
 */
@Service
@RequiredArgsConstructor
public class QwenChatCompletionService {

    private final QwenProperties properties;

    public String chat(List<QwenChatMessage> messages) {
        return chat(messages, properties.getChat().getTimeoutMillis());
    }

    public String chat(List<QwenChatMessage> messages, int timeoutMillis) {
        RestClient restClient = RestClient.builder()
                .baseUrl(removeTrailingSlash(properties.getBaseUrl()))
                .requestFactory(requestFactory(timeoutMillis))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        ChatResponse response = restClient.post()
                .uri("/chat/completions")
                .body(new ChatCompletionRequest(properties.getChat().getModel(), messages, properties.getChat().getTemperature()))
                .retrieve()
                .body(ChatResponse.class);
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Qwen chat response is empty");
        }
        return response.choices().get(0).message().content();
    }

    private SimpleClientHttpRequestFactory requestFactory(int timeoutMillis) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }

    private String removeTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record ChatCompletionRequest(String model, List<QwenChatMessage> messages, Double temperature) {
    }

    private record ChatResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
