package com.bank.aiassistant.performance;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 性能保护健康检查。
 *
 * 运维可通过 /actuator/health 观察检索舱壁余量和 Embedding 缓存大小。
 */
@Component("performanceProtection")
public class PerformanceHealthIndicator implements HealthIndicator {

    private final RetrievalBulkhead retrievalBulkhead;
    private final EmbeddingCacheService embeddingCacheService;

    public PerformanceHealthIndicator(RetrievalBulkhead retrievalBulkhead, EmbeddingCacheService embeddingCacheService) {
        this.retrievalBulkhead = retrievalBulkhead;
        this.embeddingCacheService = embeddingCacheService;
    }

    @Override
    public Health health() {
        int availablePermits = retrievalBulkhead.availablePermits();
        Health.Builder builder = availablePermits > 0 ? Health.up() : Health.status("DEGRADED");
        return builder
                .withDetail("retrievalAvailablePermits", availablePermits)
                .withDetail("embeddingCacheSize", embeddingCacheService.size())
                .build();
    }
}
