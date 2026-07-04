package com.bank.aiassistant.document.message;

import java.time.LocalDateTime;

/**
 * Message sent after a document upload commits.
 *
 * retryCount and maxRetryCount are carried in the payload because RabbitMQ requeue
 * itself cannot mutate the message body. The consumer publishes a new message with
 * retryCount + 1 when another retry is allowed.
 */
public record DocumentParseMessage(
        String documentId,
        String tenantId,
        String operatorId,
        String operatorName,
        LocalDateTime triggerTime,
        Integer retryCount,
        Integer maxRetryCount
) {
}
