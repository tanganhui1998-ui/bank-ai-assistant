package com.bank.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 性能与高可用配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.performance")
public class PerformanceProperties {

    private Async async = new Async();
    private Retrieval retrieval = new Retrieval();
    private EmbeddingCache embeddingCache = new EmbeddingCache();

    @Getter
    @Setter
    public static class Async {
        private int corePoolSize = 8;
        private int maxPoolSize = 16;
        private int queueCapacity = 200;
        private int keepAliveSeconds = 60;
    }

    @Getter
    @Setter
    public static class Retrieval {
        /**
         * 单次在线检索最大耗时预算，超过后降级为低置信结果。
         */
        private long timeoutMillis = 500;

        /**
         * 在线检索最大并发数，防止瞬时流量打满 ES 和 Embedding 服务。
         */
        private int maxConcurrent = 64;
    }

    @Getter
    @Setter
    public static class EmbeddingCache {
        private boolean enabled = true;
        private int maxSize = 1000;
        private long ttlSeconds = 300;
    }
}
