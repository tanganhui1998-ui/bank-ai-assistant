package com.bank.aiassistant.performance;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 检索链路指标服务。
 */
@Service
public class RetrievalMetricsService {

    private final MeterRegistry meterRegistry;

    public RetrievalMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String outcome, long elapsedMs, int hitCount) {
        Timer.builder("bank_ai_retrieval_latency")
                .description("RAG 在线检索耗时")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(elapsedMs, TimeUnit.MILLISECONDS);
        meterRegistry.counter("bank_ai_retrieval_requests", "outcome", outcome).increment();
        meterRegistry.summary("bank_ai_retrieval_hits", "outcome", outcome).record(hitCount);
    }
}
