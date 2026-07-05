package com.bank.aiassistant.retrieval;

import com.bank.aiassistant.config.KnowledgeElasticsearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RAG 查询改写服务。
 *
 * 当前实现采用可解释的银行术语同义词扩展，优点是稳定、低延迟、可测试。
 * 后续如果需要更强召回，可在此处接入大模型 Query Rewrite，并保留同样的输出契约。
 */
@Service
@RequiredArgsConstructor
public class RagQueryRewriteService {

    private final KnowledgeElasticsearchProperties properties;

    /**
     * 银行办公场景常见同义词词典。LinkedHashMap 用于保持改写顺序稳定，便于测试和排查。
     */
    private static final Map<String, List<String>> TERM_SYNONYMS = new LinkedHashMap<>();

    static {
        TERM_SYNONYMS.put("请假", List.of("休假", "假期", "考勤"));
        TERM_SYNONYMS.put("年假", List.of("带薪年休假", "年休假"));
        TERM_SYNONYMS.put("外包", List.of("外协", "供应商人员", "外包人员"));
        TERM_SYNONYMS.put("入场", List.of("进场", "准入", "入驻"));
        TERM_SYNONYMS.put("薪资", List.of("工资", "薪酬", "工资明细"));
        TERM_SYNONYMS.put("审批", List.of("审核", "流程", "审批节点"));
        TERM_SYNONYMS.put("报销", List.of("费用报销", "差旅报销", " reimbursement "));
        TERM_SYNONYMS.put("制度", List.of("办法", "规定", "管理办法"));
        TERM_SYNONYMS.put("权限", List.of("授权", "访问控制", "数据权限"));
        TERM_SYNONYMS.put("密级", List.of("保密等级", "秘密", "机密"));
    }

    public List<String> rewrite(String question) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        String normalized = question.trim();
        Set<String> queries = new LinkedHashSet<>();
        queries.add(normalized);
        if (!properties.isQueryRewriteEnabled()) {
            return List.copyOf(queries);
        }

        for (Map.Entry<String, List<String>> entry : TERM_SYNONYMS.entrySet()) {
            if (!normalized.contains(entry.getKey())) {
                continue;
            }
            for (String synonym : entry.getValue()) {
                if (queries.size() >= properties.getMaxRewriteQueries()) {
                    return List.copyOf(queries);
                }
                queries.add(normalized.replace(entry.getKey(), synonym.trim()));
            }
        }

        if (queries.size() < properties.getMaxRewriteQueries()) {
            String expanded = appendMatchedSynonyms(normalized);
            if (!expanded.equals(normalized)) {
                queries.add(expanded);
            }
        }
        return List.copyOf(queries).stream()
                .limit(Math.max(1, properties.getMaxRewriteQueries()))
                .toList();
    }

    private String appendMatchedSynonyms(String question) {
        List<String> matched = new ArrayList<>();
        TERM_SYNONYMS.forEach((term, synonyms) -> {
            if (question.contains(term)) {
                matched.addAll(synonyms.stream().map(String::trim).filter(StringUtils::hasText).toList());
            }
        });
        return matched.isEmpty() ? question : question + " " + String.join(" ", matched);
    }
}
