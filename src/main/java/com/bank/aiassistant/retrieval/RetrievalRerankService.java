package com.bank.aiassistant.retrieval;

import com.bank.aiassistant.config.KnowledgeElasticsearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * RAG 本地二次重排服务。
 *
 * 本服务用于把初召回结果从“能召回”进一步调到“更相关”：
 * 1. 保留 RRF 原始分，确保两路检索融合结果仍是主依据。
 * 2. 标题路径命中问题关键词时加权，增强章节级溯源的稳定性。
 * 3. 正文命中问题关键词时加权，避免纯向量相似但文字不匹配的结果过前。
 * 4. 引入切片质量分，降低解析噪声切片的排名。
 */
@Service
@RequiredArgsConstructor
public class RetrievalRerankService {

    private final KnowledgeElasticsearchProperties properties;

    public double rerank(String question, double rrfScore, Map<String, Object> source) {
        if (!properties.isRerankEnabled()) {
            return rrfScore;
        }
        Set<String> keywords = extractKeywords(question);
        String content = stringValue(source.get("content")).toLowerCase(Locale.ROOT);
        String chapterPath = stringValue(source.get("chapterPath")).toLowerCase(Locale.ROOT);

        double score = rrfScore;
        for (String keyword : keywords) {
            String lowered = keyword.toLowerCase(Locale.ROOT);
            if (chapterPath.contains(lowered)) {
                score += properties.getTitleMatchBoost();
            }
            if (content.contains(lowered)) {
                score += properties.getContentMatchBoost();
            }
        }
        score += qualityScore(source) * properties.getQualityScoreBoost();
        return score;
    }

    Set<String> extractKeywords(String question) {
        Set<String> keywords = new LinkedHashSet<>();
        if (question == null) {
            return keywords;
        }
        for (String part : question.split("[\\s,，。！？；;、]+")) {
            String value = part.trim();
            if (value.length() >= 2) {
                keywords.add(value);
            }
        }
        if (keywords.isEmpty() && question.trim().length() >= 2) {
            keywords.add(question.trim());
        }
        return keywords;
    }

    private double qualityScore(Map<String, Object> source) {
        Object value = source.get("qualityScore");
        if (value instanceof Number number) {
            return Math.max(0D, Math.min(1D, number.doubleValue()));
        }
        return 0.5D;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
