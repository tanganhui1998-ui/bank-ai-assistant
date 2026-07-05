package com.bank.aiassistant.retrieval;

import com.bank.aiassistant.config.KnowledgeElasticsearchProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagQueryRewriteServiceTest {

    @Test
    void rewriteShouldExpandBankSynonymsAndKeepOriginalFirst() {
        KnowledgeElasticsearchProperties properties = new KnowledgeElasticsearchProperties();
        properties.setQueryRewriteEnabled(true);
        properties.setMaxRewriteQueries(3);
        RagQueryRewriteService service = new RagQueryRewriteService(properties);

        List<String> queries = service.rewrite("外包人员入场需要哪些材料");

        assertThat(queries).hasSize(3);
        assertThat(queries.get(0)).isEqualTo("外包人员入场需要哪些材料");
        assertThat(queries).anyMatch(query -> query.contains("外协"));
    }

    @Test
    void rewriteShouldOnlyReturnOriginalWhenDisabled() {
        KnowledgeElasticsearchProperties properties = new KnowledgeElasticsearchProperties();
        properties.setQueryRewriteEnabled(false);
        RagQueryRewriteService service = new RagQueryRewriteService(properties);

        assertThat(service.rewrite("请假流程")).containsExactly("请假流程");
    }
}
