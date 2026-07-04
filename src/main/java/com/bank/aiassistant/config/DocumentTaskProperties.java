package com.bank.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.document")
public class DocumentTaskProperties {

    /**
     * Main direct exchange for parse task messages.
     */
    private String parseExchange;

    /**
     * Main queue consumed by the parse worker.
     */
    private String parseQueue;

    /**
     * Routing key from main exchange to main queue.
     */
    private String parseRoutingKey;

    /**
     * Dead-letter exchange for permanently failed or expired parse messages.
     */
    private String parseDeadLetterExchange;

    /**
     * Dead-letter queue inspected by the DLQ listener and operations tooling.
     */
    private String parseDeadLetterQueue;

    /**
     * Routing key from DLX to dead-letter queue.
     */
    private String parseDeadLetterRoutingKey;

    /**
     * Message TTL on the main parse queue, in milliseconds.
     */
    private Long parseMessageTtlMillis = 60000L;

    /**
     * Maximum business retry count carried in DocumentParseMessage.
     */
    private Integer parseMaxRetryCount = 3;

    /**
     * Elasticsearch index name for document chunks, used by the future parser.
     */
    private String chunkIndexName;
}
