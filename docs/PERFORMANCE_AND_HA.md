# 阶段七：性能与高可用

## 本阶段新增能力

1. Actuator：新增 `/actuator/health`、`/actuator/metrics`、`/actuator/prometheus`。
2. 线程池参数化：`retrievalExecutor` 支持通过环境变量调整核心线程、最大线程、队列容量。
3. 检索舱壁：限制 RAG 在线检索最大并发，流量过高时快速返回低置信提示。
4. 检索超时降级：BM25 和向量召回单路超时后自动返回空召回，避免整体请求被拖挂。
5. Embedding 缓存：对在线查询向量做本地短 TTL 缓存，减少重复调用通义 Embedding API。
6. 指标采集：记录检索耗时、请求结果分类和命中数量。
7. 健康检查：暴露检索舱壁剩余许可和 Embedding 缓存大小。

## 关键配置

```yaml
app:
  performance:
    async:
      core-pool-size: 8
      max-pool-size: 16
      queue-capacity: 200
    retrieval:
      timeout-millis: 500
      max-concurrent: 64
    embedding-cache:
      enabled: true
      max-size: 1000
      ttl-seconds: 300
```

## 运维建议

1. 生产至少部署 2 个应用实例，前置网关做健康检查和流量切换。
2. RabbitMQ、MySQL、Redis、Elasticsearch 使用托管版或主从/集群模式。
3. 将 `/actuator/prometheus` 接入 Prometheus，重点监控 `bank_ai_retrieval_latency` 和 `bank_ai_retrieval_requests`。
4. 当 `bulkhead_rejected` 增多时，优先检查 ES 延迟、Embedding API 延迟和线程池队列。
5. 文档解析任务建议独立部署 worker 实例，避免大文件解析影响在线对话。
