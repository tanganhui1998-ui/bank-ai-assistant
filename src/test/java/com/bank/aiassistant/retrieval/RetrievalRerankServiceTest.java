package com.bank.aiassistant.retrieval;

import com.bank.aiassistant.config.KnowledgeElasticsearchProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalRerankServiceTest {

    @Test
    void rerankShouldBoostTitleContentAndQualityMatches() {
        KnowledgeElasticsearchProperties properties = new KnowledgeElasticsearchProperties();
        RetrievalRerankService service = new RetrievalRerankService(properties);

        double score = service.rerank("外包 入场", 0.02D, Map.of(
                "chapterPath", "外包人员管理办法 > 入场管理",
                "content", "外包人员入场前需要提交供应商资质和材料清单。",
                "qualityScore", 0.9D
        ));

        assertThat(score).isGreaterThan(0.02D);
    }

    @Test
    void rerankShouldKeepOriginalScoreWhenDisabled() {
        KnowledgeElasticsearchProperties properties = new KnowledgeElasticsearchProperties();
        properties.setRerankEnabled(false);
        RetrievalRerankService service = new RetrievalRerankService(properties);

        assertThat(service.rerank("薪资", 0.02D, Map.of("content", "薪资明细"))).isEqualTo(0.02D);
    }
}
