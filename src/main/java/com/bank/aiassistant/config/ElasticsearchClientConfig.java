package com.bank.aiassistant.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch Java Client 8.x 配置。
 *
 * 这里直接配置底层 RestClient 的连接池参数，避免高并发批量写入时频繁创建连接。
 */
@Configuration
@EnableConfigurationProperties(KnowledgeElasticsearchProperties.class)
public class ElasticsearchClientConfig {

    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient(
            ElasticsearchProperties elasticsearchProperties,
            KnowledgeElasticsearchProperties knowledgeProperties
    ) {
        HttpHost[] hosts = elasticsearchProperties.getUris().stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(hosts)
                .setRequestConfigCallback(requestConfig -> requestConfig
                        .setConnectTimeout(knowledgeProperties.getConnectTimeoutMillis())
                        .setSocketTimeout(knowledgeProperties.getSocketTimeoutMillis()))
                .setHttpClientConfigCallback(httpClient -> httpClient
                        .setMaxConnTotal(knowledgeProperties.getMaxConnTotal())
                        .setMaxConnPerRoute(knowledgeProperties.getMaxConnPerRoute()));

        if (StringUtils.hasText(elasticsearchProperties.getUsername())) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(
                            elasticsearchProperties.getUsername(),
                            elasticsearchProperties.getPassword()
                    )
            );
            builder.setHttpClientConfigCallback(httpClient -> httpClient
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setMaxConnTotal(knowledgeProperties.getMaxConnTotal())
                    .setMaxConnPerRoute(knowledgeProperties.getMaxConnPerRoute()));
        }

        return builder.build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient elasticsearchRestClient) {
        return new RestClientTransport(elasticsearchRestClient, new JacksonJsonpMapper());
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport elasticsearchTransport) {
        return new ElasticsearchClient(elasticsearchTransport);
    }
}
