package com.bank.aiassistant.embedding;

import com.bank.aiassistant.config.QwenProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 通义千问 text-embedding-v2 向量化服务。
 *
 * 这里使用 DashScope OpenAI-compatible Embeddings API：
 * POST {baseUrl}/embeddings
 *
 * 服务特性：
 * - 单批最多 100 条，由配置 qwen.embedding.max-batch-size 控制。
 * - 输入文本会做长度截断，避免超过模型输入上限。
 * - 失败自动重试 3 次，仍失败则抛出异常，让上层 RabbitMQ 重试解析任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QwenEmbeddingService implements EmbeddingService {

    private final QwenProperties properties;

    @Override
    public List<Float> embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<List<Float>> result = new ArrayList<>();
        int batchSize = Math.min(properties.getEmbedding().getMaxBatchSize(), 100);
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            result.addAll(callWithRetry(texts.subList(start, end)));
        }
        return result;
    }

    private List<List<Float>> callWithRetry(List<String> texts) {
        RuntimeException lastException = null;
        int maxRetries = Math.max(1, properties.getEmbedding().getMaxRetries());
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return callEmbeddingApi(texts);
            } catch (RuntimeException ex) {
                lastException = ex;
                log.warn("Qwen embedding API call failed, attempt={}, maxRetries={}, batchSize={}",
                        attempt, maxRetries, texts.size(), ex);
                sleepBeforeRetry(attempt);
            }
        }
        throw new IllegalStateException("Qwen embedding API failed after retries", lastException);
    }

    private List<List<Float>> callEmbeddingApi(List<String> texts) {
        RestClient restClient = RestClient.builder()
                .baseUrl(removeTrailingSlash(properties.getBaseUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        EmbeddingRequest request = new EmbeddingRequest(
                properties.getEmbedding().getModel(),
                texts.stream().map(this::truncateInput).toList()
        );

        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null || response.data().size() != texts.size()) {
            throw new IllegalStateException("Invalid embedding response size");
        }

        return response.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingData::index))
                .map(EmbeddingData::embedding)
                .toList();
    }

    /**
     * text-embedding-v2 输入上限按 token 计算，这里用字符数做保守截断。
     */
    private String truncateInput(String text) {
        String value = text == null ? "" : text;
        int maxChars = properties.getEmbedding().getMaxInputChars();
        return value.length() <= maxChars ? value : value.substring(0, maxChars);
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(Duration.ofMillis(300L * attempt).toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding retry interrupted", interruptedException);
        }
    }

    private String removeTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record EmbeddingRequest(String model, List<String> input) {
    }

    private record EmbeddingResponse(List<EmbeddingData> data) {
    }

    private record EmbeddingData(int index, List<Float> embedding) {
    }
}
