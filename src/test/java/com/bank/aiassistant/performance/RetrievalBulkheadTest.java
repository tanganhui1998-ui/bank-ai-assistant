package com.bank.aiassistant.performance;

import com.bank.aiassistant.config.PerformanceProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalBulkheadTest {

    @Test
    void bulkheadShouldRejectWhenNoPermitAvailableThenRecoverAfterRelease() {
        PerformanceProperties properties = new PerformanceProperties();
        properties.getRetrieval().setMaxConcurrent(1);
        RetrievalBulkhead bulkhead = new RetrievalBulkhead(properties);

        assertThat(bulkhead.tryAcquire()).isTrue();
        assertThat(bulkhead.tryAcquire()).isFalse();

        bulkhead.release();

        assertThat(bulkhead.tryAcquire()).isTrue();
    }
}
