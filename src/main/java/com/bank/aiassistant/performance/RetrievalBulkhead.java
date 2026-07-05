package com.bank.aiassistant.performance;

import com.bank.aiassistant.config.PerformanceProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/**
 * 在线检索舱壁。
 *
 * 用信号量限制同时进入 RAG 检索链路的请求数，避免高峰流量把 ES、Embedding
 * 和线程池全部压垮。无法获得许可时由上层快速降级。
 */
@Component
public class RetrievalBulkhead {

    private final Semaphore semaphore;

    public RetrievalBulkhead(PerformanceProperties properties) {
        this.semaphore = new Semaphore(Math.max(1, properties.getRetrieval().getMaxConcurrent()));
    }

    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    public void release() {
        semaphore.release();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
