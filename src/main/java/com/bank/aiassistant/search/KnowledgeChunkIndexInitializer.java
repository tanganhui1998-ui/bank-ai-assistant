package com.bank.aiassistant.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.bank.aiassistant.config.KnowledgeElasticsearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 知识库切片索引初始化器。
 *
 * 启动时检查索引是否存在，不存在则创建 Mapping。IK 分词器需要 ES 集群已安装
 * analysis-ik 插件；如果未安装，索引创建会失败并在启动日志中暴露出来，便于运维处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeChunkIndexInitializer implements ApplicationRunner {

    private final ElasticsearchClient elasticsearchClient;
    private final RestClient restClient;
    private final KnowledgeElasticsearchProperties properties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String indexName = properties.getChunkIndexName();
        boolean exists = elasticsearchClient.indices()
                .exists(request -> request.index(indexName))
                .value();
        if (exists) {
            log.info("Knowledge chunk index already exists, indexName={}", indexName);
            return;
        }

        Request request = new Request("PUT", "/" + indexName);
        request.setJsonEntity(mappingJson());
        restClient.performRequest(request);
        log.info("Created knowledge chunk index, indexName={}", indexName);
    }

    private String mappingJson() throws IOException {
        return """
                {
                  "mappings": {
                    "properties": {
                      "chunkId": { "type": "keyword" },
                      "documentId": { "type": "keyword" },
                      "documentName": { "type": "keyword" },
                      "documentType": { "type": "keyword" },
                      "versionNo": { "type": "keyword" },
                      "department": { "type": "keyword" },
                      "confidentialityLevel": { "type": "keyword" },
                      "status": { "type": "keyword" },
                      "chunkStatus": { "type": "keyword" },
                      "latestVersion": { "type": "boolean" },
                      "content": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart"
                      },
                      "embedding": {
                        "type": "dense_vector",
                        "dims": 1536,
                        "index": true,
                        "similarity": "cosine"
                      },
                      "chapterPath": {
                        "type": "text",
                        "analyzer": "ik_smart",
                        "search_analyzer": "ik_smart"
                      },
                      "chapterNo": { "type": "keyword" },
                      "chunkSeq": { "type": "integer" },
                      "startPage": { "type": "integer" },
                      "endPage": { "type": "integer" },
                      "qualityScore": { "type": "double" },
                      "effectiveTime": { "type": "date", "format": "strict_date_optional_time||epoch_millis" },
                      "publishedTime": { "type": "date", "format": "strict_date_optional_time||epoch_millis" },
                      "createdTime": { "type": "date", "format": "strict_date_optional_time||epoch_millis" }
                    }
                  }
                }
                """;
    }
}
