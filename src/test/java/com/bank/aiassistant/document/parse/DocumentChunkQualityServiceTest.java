package com.bank.aiassistant.document.parse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkQualityServiceTest {

    private final DocumentChunkQualityService service = new DocumentChunkQualityService();

    @Test
    void scoreShouldPreferInformativePolicyText() {
        String text = "第一章 总则\n为规范员工考勤管理，明确请假、审批、销假等流程，保障经营管理秩序，制定本办法。";

        assertThat(service.score(text)).isGreaterThan(0.35D);
    }

    @Test
    void scoreShouldRejectBlankOrNoisyText() {
        assertThat(service.score("")).isZero();
        assertThat(service.score(",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,")).isLessThan(0.35D);
    }
}
