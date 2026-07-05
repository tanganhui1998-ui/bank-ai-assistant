package com.bank.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.elasticsearch")
public class KnowledgeElasticsearchProperties {

    /**
     * 知识库切片索引名称。
     */
    private String chunkIndexName = "bank-ai-document-chunk";

    /**
     * ES HTTP 连接池最大连接数。
     */
    private int maxConnTotal = 100;

    /**
     * ES HTTP 连接池单路由最大连接数。
     */
    private int maxConnPerRoute = 50;

    /**
     * 连接超时时间，单位毫秒。
     */
    private int connectTimeoutMillis = 5000;

    /**
     * Socket 读取超时时间，单位毫秒。
     */
    private int socketTimeoutMillis = 30000;

    /**
     * 在线检索每路召回数量。BM25 和向量检索各取该数量，再做 RRF 融合。
     */
    private int hybridRecallSize = 50;

    /**
     * RRF 融合参数，公式为 score = sum(1 / (rrfK + rank))。
     */
    private int rrfK = 60;

    /**
     * 在线检索低置信阈值，融合分数低于该值的结果不返回。
     */
    private double minRrfScore = 0.012D;

    /**
     * 标题路径命中问题关键词时的加权增量。
     */
    private double titleMatchBoost = 0.03D;

    /**
     * 是否启用查询改写。开启后会基于银行术语词典生成少量扩展查询，提升召回率。
     */
    private boolean queryRewriteEnabled = true;

    /**
     * 单次检索最多使用多少条改写查询，避免 BM25 查询 fan-out 过大。
     */
    private int maxRewriteQueries = 3;

    /**
     * 是否启用本地二次重排。重排会综合 RRF 分数、正文命中、标题命中和切片质量分。
     */
    private boolean rerankEnabled = true;

    /**
     * 正文命中问题关键词时的重排加权增量。
     */
    private double contentMatchBoost = 0.015D;

    /**
     * 切片质量分在重排中的权重，避免低质量切片在召回结果中排得过前。
     */
    private double qualityScoreBoost = 0.02D;
}
