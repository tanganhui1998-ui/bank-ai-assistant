package com.bank.aiassistant.performance;

import com.bank.aiassistant.config.PerformanceProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Embedding 本地短 TTL 缓存。
 *
 * 在线检索中用户常会重复问相同问题，缓存可减少对通义 Embedding API 的重复调用。
 * 这里只缓存查询向量，不缓存文档切片向量，避免大批量入库时占用过多内存。
 */
@Component
public class EmbeddingCacheService {

    private final PerformanceProperties.EmbeddingCache properties;
    private final Map<String, CacheValue> cache;

    public EmbeddingCacheService(PerformanceProperties properties) {
        this.properties = properties.getEmbeddingCache();
        this.cache = new LinkedHashMap<>(64, 0.75F, true);
    }

    public synchronized Optional<List<Float>> get(String text) {
        if (!properties.isEnabled() || text == null) {
            return Optional.empty();
        }
        CacheValue value = cache.get(text);
        if (value == null || value.expireAt().isBefore(Instant.now())) {
            cache.remove(text);
            return Optional.empty();
        }
        return Optional.of(value.embedding());
    }

    public synchronized void put(String text, List<Float> embedding) {
        if (!properties.isEnabled() || text == null || embedding == null) {
            return;
        }
        while (cache.size() >= Math.max(1, properties.getMaxSize())) {
            String eldestKey = cache.keySet().iterator().next();
            cache.remove(eldestKey);
        }
        cache.put(text, new CacheValue(embedding, Instant.now().plusSeconds(properties.getTtlSeconds())));
    }

    public synchronized int size() {
        return cache.size();
    }

    private record CacheValue(List<Float> embedding, Instant expireAt) {
    }
}
