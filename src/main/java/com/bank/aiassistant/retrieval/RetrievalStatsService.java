package com.bank.aiassistant.retrieval;

import com.bank.aiassistant.repository.RetrievalAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 检索监控统计服务。
 *
 * 用于知识库运营：高频问题可以沉淀 FAQ，低命中问题可以提示补充制度材料。
 */
@Service
@RequiredArgsConstructor
public class RetrievalStatsService {

    private final RetrievalAuditLogRepository repository;

    public List<RetrievalStatsItem> highFrequencyQueries(int limit) {
        return repository.findHighFrequencyQueries(normalizeLimit(limit)).stream()
                .map(this::toItem)
                .toList();
    }

    public List<RetrievalStatsItem> lowHitQueries(int limit) {
        return repository.findLowHitQueries(normalizeLimit(limit)).stream()
                .map(this::toItem)
                .toList();
    }

    private RetrievalStatsItem toItem(Map<String, Object> row) {
        return RetrievalStatsItem.builder()
                .question(String.valueOf(row.get("question")))
                .queryCount(((Number) row.get("query_count")).longValue())
                .avgHitCount(((Number) row.get("avg_hit_count")).doubleValue())
                .build();
    }

    private int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 100);
    }
}
