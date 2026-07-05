package com.bank.aiassistant.performance;

import com.bank.aiassistant.config.PerformanceProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingCacheServiceTest {

    @Test
    void cacheShouldReturnValueBeforeTtlExpires() {
        PerformanceProperties properties = new PerformanceProperties();
        properties.getEmbeddingCache().setTtlSeconds(60);
        EmbeddingCacheService service = new EmbeddingCacheService(properties);

        service.put("请假制度", List.of(0.1F, 0.2F));

        assertThat(service.get("请假制度")).contains(List.of(0.1F, 0.2F));
        assertThat(service.size()).isEqualTo(1);
    }

    @Test
    void cacheShouldEvictEldestWhenMaxSizeReached() {
        PerformanceProperties properties = new PerformanceProperties();
        properties.getEmbeddingCache().setMaxSize(1);
        EmbeddingCacheService service = new EmbeddingCacheService(properties);

        service.put("q1", List.of(0.1F));
        service.put("q2", List.of(0.2F));

        assertThat(service.get("q1")).isEmpty();
        assertThat(service.get("q2")).contains(List.of(0.2F));
    }
}
