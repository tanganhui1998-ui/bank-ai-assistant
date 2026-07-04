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
}
