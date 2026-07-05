package com.bank.aiassistant.retrieval;

import com.bank.aiassistant.config.KnowledgeElasticsearchProperties;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.bank.aiassistant.embedding.EmbeddingService;
import com.bank.aiassistant.security.PermissionFilterService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 助手在线混合检索服务。
 *
 * 检索流程：
 * 1. BM25 全文检索和问题向量化并行启动。
 * 2. 问题向量生成后执行 ES KNN 向量检索。
 * 3. 两路各取 Top 50。
 * 4. 使用 RRF 倒数排名融合合并结果。
 * 5. 应用标题路径命中加权和低置信阈值过滤。
 * 6. 生成高亮、引用来源和异步审计日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineRetrievalService {

    private static final String LOW_CONFIDENCE_MESSAGE = "知识库中未找到相关信息，建议咨询相关部门。";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KnowledgeElasticsearchProperties properties;
    private final PermissionFilterService permissionFilterService;
    private final EmbeddingService embeddingService;
    private final CurrentUserProvider currentUserProvider;
    private final RetrievalAuditService retrievalAuditService;
    private final RagQueryRewriteService queryRewriteService;
    private final RetrievalRerankService retrievalRerankService;

    @Qualifier("retrievalExecutor")
    private final Executor retrievalExecutor;

    public RetrievalResponse retrieve(RetrievalRequest request) {
        long start = System.currentTimeMillis();
        CurrentUser currentUser = currentUserProvider.currentUser();
        int topK = normalizeTopK(request.getTopK());
        List<Map<String, Object>> filters = buildFilters(request.getFilters());
        List<String> rewrittenQueries = queryRewriteService.rewrite(request.getQuestion());

        AtomicLong bm25Ms = new AtomicLong();
        AtomicLong embeddingMs = new AtomicLong();
        AtomicLong vectorMs = new AtomicLong();

        CompletableFuture<List<EsHit>> bm25Future = CompletableFuture.supplyAsync(
                () -> {
                    long bm25Start = System.currentTimeMillis();
                    try {
                        return bm25Search(rewrittenQueries, filters);
                    } finally {
                        bm25Ms.set(System.currentTimeMillis() - bm25Start);
                    }
                }, retrievalExecutor);
        CompletableFuture<List<Float>> queryVectorFuture = CompletableFuture.supplyAsync(
                () -> {
                    long embeddingStart = System.currentTimeMillis();
                    try {
                        return embeddingService.embed(request.getQuestion());
                    } finally {
                        embeddingMs.set(System.currentTimeMillis() - embeddingStart);
                    }
                }, retrievalExecutor);
        CompletableFuture<List<EsHit>> vectorFuture = queryVectorFuture.thenApplyAsync(
                vector -> {
                    long vectorStart = System.currentTimeMillis();
                    try {
                        return vectorSearch(vector, filters);
                    } finally {
                        vectorMs.set(System.currentTimeMillis() - vectorStart);
                    }
                }, retrievalExecutor);

        List<EsHit> bm25Hits = bm25Future.join();
        List<EsHit> vectorHits = vectorFuture.join();
        long fusionStart = System.currentTimeMillis();
        BuiltResults builtResults = fuseAndBuildResults(request.getQuestion(), bm25Hits, vectorHits, topK);
        List<RetrievalResultItem> results = builtResults.results();
        long fusionMs = System.currentTimeMillis() - fusionStart;

        boolean lowConfidence = results.isEmpty();
        RetrievalResponse response = RetrievalResponse.builder()
                .question(request.getQuestion())
                .results(results)
                .citations(results.stream().map(item -> item.citation().formatted()).distinct().toList())
                .lowConfidence(lowConfidence)
                .message(lowConfidence ? LOW_CONFIDENCE_MESSAGE : null)
                .elapsedMs(System.currentTimeMillis() - start)
                .trace(RetrievalTrace.builder()
                        .rewrittenQueries(rewrittenQueries)
                        .timings(Map.of(
                                "bm25Ms", bm25Ms.get(),
                                "embeddingMs", embeddingMs.get(),
                                "vectorMs", vectorMs.get(),
                                "fusionMs", fusionMs))
                        .bm25HitCount(bm25Hits.size())
                        .vectorHitCount(vectorHits.size())
                        .fusedHitCount(builtResults.fusedHitCount())
                        .build())
                .build();

        retrievalAuditService.record(currentUser, response);
        log.info("Online retrieval finished, userId={}, question={}, rewrittenQueries={}, hitCount={}, elapsedMs={}, trace={}, documentIds={}",
                currentUser.userId(), request.getQuestion(), rewrittenQueries, results.size(), response.elapsedMs(), response.trace(),
                results.stream().map(RetrievalResultItem::documentId).distinct().toList());
        if (lowConfidence) {
            log.warn("Low confidence retrieval, userId={}, question={}, elapsedMs={}",
                    currentUser.userId(), request.getQuestion(), response.elapsedMs());
        }
        return response;
    }

    private List<Map<String, Object>> buildFilters(RetrievalFilters extraFilters) {
        List<Map<String, Object>> filters = new ArrayList<>(permissionFilterService.buildEsFilterDsl());
        if (extraFilters == null) {
            return filters;
        }
        addTermFilter(filters, "documentType", extraFilters.getDocumentType());
        addTermFilter(filters, "department", extraFilters.getDepartment());
        addTermFilter(filters, "versionNo", extraFilters.getVersionNo());
        addTermFilter(filters, "documentId", extraFilters.getDocumentId());
        return filters;
    }

    private void addTermFilter(List<Map<String, Object>> filters, String field, String value) {
        if (StringUtils.hasText(value)) {
            filters.add(Map.of("term", Map.of(field, value)));
        }
    }

    private List<EsHit> bm25Search(List<String> queries, List<Map<String, Object>> filters) {
        Map<String, EsHit> merged = new LinkedHashMap<>();
        for (String query : queries) {
            bm25SearchOne(query, filters).forEach(hit -> merged.putIfAbsent(hit.chunkId(), hit));
        }
        return new ArrayList<>(merged.values()).stream()
                .limit(properties.getHybridRecallSize())
                .toList();
    }

    private List<EsHit> bm25SearchOne(String question, List<Map<String, Object>> filters) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", properties.getHybridRecallSize());
        body.put("_source", Map.of("excludes", List.of("embedding")));
        body.put("query", Map.of("bool", Map.of(
                "must", List.of(Map.of("multi_match", Map.of(
                        "query", question,
                        "fields", List.of("content^1.0", "chapterPath^2.0")))),
                "filter", filters)));
        body.put("highlight", Map.of(
                "pre_tags", List.of("<em>"),
                "post_tags", List.of("</em>"),
                "fields", Map.of("content", Map.of("fragment_size", 160, "number_of_fragments", 2))));
        return executeSearch(body);
    }

    private List<EsHit> vectorSearch(List<Float> vector, List<Map<String, Object>> filters) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", properties.getHybridRecallSize());
        body.put("_source", Map.of("excludes", List.of("embedding")));
        body.put("knn", Map.of(
                "field", "embedding",
                "query_vector", vector,
                "k", properties.getHybridRecallSize(),
                "num_candidates", Math.max(100, properties.getHybridRecallSize() * 3),
                "filter", filters));
        return executeSearch(body);
    }

    private List<EsHit> executeSearch(Map<String, Object> body) {
        try {
            Request request = new Request("POST", "/" + properties.getChunkIndexName() + "/_search");
            request.setJsonEntity(objectMapper.writeValueAsString(body));
            Response response = restClient.performRequest(request);
            try (InputStream inputStream = response.getEntity().getContent()) {
                Map<String, Object> json = objectMapper.readValue(inputStream, new TypeReference<>() {
                });
                return parseHits(json);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("ES retrieval query failed", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<EsHit> parseHits(Map<String, Object> json) {
        Map<String, Object> hitsWrapper = (Map<String, Object>) json.getOrDefault("hits", Map.of());
        List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsWrapper.getOrDefault("hits", List.of());
        List<EsHit> result = new ArrayList<>();
        for (Map<String, Object> hit : hits) {
            Map<String, Object> source = (Map<String, Object>) hit.getOrDefault("_source", Map.of());
            Map<String, Object> highlight = (Map<String, Object>) hit.getOrDefault("highlight", Map.of());
            result.add(new EsHit(
                    stringValue(source.get("chunkId")),
                    source,
                    ((Number) hit.getOrDefault("_score", 0D)).doubleValue(),
                    highlight
            ));
        }
        return result;
    }

    private BuiltResults fuseAndBuildResults(
            String question,
            List<EsHit> bm25Hits,
            List<EsHit> vectorHits,
            int topK
    ) {
        Map<String, FusedHit> fused = new LinkedHashMap<>();
        applyRrf(fused, bm25Hits);
        applyRrf(fused, vectorHits);
        Set<String> keywords = extractKeywords(question);
        fused.values().forEach(hit -> hit.rerank(question));

        List<RetrievalResultItem> results = fused.values().stream()
                .filter(hit -> hit.score >= properties.getMinRrfScore())
                .sorted(Comparator.comparingDouble(FusedHit::score).reversed())
                .limit(topK)
                .map(hit -> toResultItem(hit, keywords))
                .toList();
        return new BuiltResults(results, fused.size());
    }

    private void applyRrf(Map<String, FusedHit> fused, List<EsHit> hits) {
        for (int i = 0; i < hits.size(); i++) {
            EsHit hit = hits.get(i);
            int rank = i + 1;
            double rrfScore = 1D / (properties.getRrfK() + rank);
            fused.computeIfAbsent(hit.chunkId(), key -> new FusedHit(hit)).addScore(rrfScore);
        }
    }

    private RetrievalResultItem toResultItem(FusedHit fusedHit, Set<String> keywords) {
        EsHit hit = fusedHit.hit;
        String content = stringValue(hit.source().get("content"));
        CitationSource citation = buildCitation(hit.source());
        return RetrievalResultItem.builder()
                .chunkId(hit.chunkId())
                .documentId(stringValue(hit.source().get("documentId")))
                .content(content)
                .highlightedContent(resolveHighlight(hit, content, keywords))
                .score(fusedHit.score())
                .citation(citation)
                .build();
    }

    @SuppressWarnings("unchecked")
    private String resolveHighlight(EsHit hit, String content, Set<String> keywords) {
        Object highlightContent = hit.highlight().get("content");
        if (highlightContent instanceof List<?> fragments && !fragments.isEmpty()) {
            return String.join(" ... ", fragments.stream().map(String::valueOf).toList());
        }
        String highlighted = content;
        for (String keyword : keywords) {
            if (keyword.length() >= 2) {
                highlighted = highlighted.replace(keyword, "<em>" + keyword + "</em>");
            }
        }
        return highlighted;
    }

    private CitationSource buildCitation(Map<String, Object> source) {
        String documentName = stringValue(source.get("documentName"));
        String chapterPath = stringValue(source.get("chapterPath"));
        Integer pageNo = intValue(source.get("startPage"));
        String documentType = stringValue(source.get("documentType"));
        String versionNo = stringValue(source.get("versionNo"));
        String formatted = "《" + documentName + "》" + blankToEmpty(chapterPath)
                + (pageNo == null ? "" : " 第" + pageNo + "页");
        return CitationSource.builder()
                .documentName(documentName)
                .chapterPath(chapterPath)
                .pageNo(pageNo)
                .documentType(documentType)
                .versionNo(versionNo)
                .formatted(formatted)
                .build();
    }

    private Set<String> extractKeywords(String question) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String part : question.split("[\\s,，。！？；;、]+")) {
            String value = part.trim();
            if (value.length() >= 2) {
                keywords.add(value);
            }
        }
        if (keywords.isEmpty() && question.length() >= 2) {
            keywords.add(question);
        }
        return keywords;
    }

    private int normalizeTopK(Integer topK) {
        return Math.min(Math.max(topK == null ? 5 : topK, 1), 20);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Integer intValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    private record EsHit(String chunkId, Map<String, Object> source, double rawScore, Map<String, Object> highlight) {
    }

    private class FusedHit {
        private final EsHit hit;
        private double score;

        FusedHit(EsHit hit) {
            this.hit = hit;
        }

        void addScore(double value) {
            this.score += value;
        }

        void rerank(String question) {
            score = retrievalRerankService.rerank(question, score, hit.source());
        }

        double score() {
            return score;
        }
    }

    private record BuiltResults(List<RetrievalResultItem> results, int fusedHitCount) {
    }
}
