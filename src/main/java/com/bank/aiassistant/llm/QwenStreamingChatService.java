package com.bank.aiassistant.llm;

import com.bank.aiassistant.config.QwenProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通义千问流式 Chat 调用服务。
 *
 * 使用 OpenAI-compatible SSE 响应格式，每收到一个 delta.content 就回调给上层，
 * 上层再通过 SseEmitter 推送给前端。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QwenStreamingChatService {

    private final QwenProperties properties;
    private final ObjectMapper objectMapper;

    public void stream(List<QwenChatMessage> messages, Consumer<String> onToken) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(properties.getChat().getTimeoutMillis()))
                    .build();
            String body = objectMapper.writeValueAsString(new StreamRequest(
                    properties.getChat().getModel(),
                    messages,
                    properties.getChat().getTemperature(),
                    true
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(removeTrailingSlash(properties.getBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<java.util.stream.Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Qwen stream call failed, status=" + response.statusCode());
            }
            response.body().forEach(line -> handleLine(line, onToken));
        } catch (Exception ex) {
            log.error("Qwen streaming chat failed", ex);
            throw new IllegalStateException("Qwen streaming chat failed", ex);
        }
    }

    private void handleLine(String line, Consumer<String> onToken) {
        try {
            if (line == null || !line.startsWith("data:")) {
                return;
            }
            String data = line.substring("data:".length()).trim();
            if ("[DONE]".equals(data) || data.isBlank()) {
                return;
            }
            JsonNode root = objectMapper.readTree(data);
            JsonNode content = root.path("choices").path(0).path("delta").path("content");
            if (!content.isMissingNode() && !content.isNull()) {
                onToken.accept(content.asText());
            }
        } catch (Exception ex) {
            log.warn("Failed to parse Qwen stream line: {}", line, ex);
        }
    }

    private String removeTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record StreamRequest(String model, List<QwenChatMessage> messages, Double temperature, boolean stream) {
    }
}
