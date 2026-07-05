package com.bank.aiassistant.retrieval;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 单次 RAG 检索的可观测追踪信息。
 *
 * 该对象返回给调用方用于排查召回效果，也可被前端或运营面板采集展示。
 */
@Builder
public record RetrievalTrace(
        List<String> rewrittenQueries,
        Map<String, Long> timings,
        int bm25HitCount,
        int vectorHitCount,
        int fusedHitCount
) {
}
