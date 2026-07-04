package com.bank.aiassistant.document.message;

import com.bank.aiassistant.config.DocumentTaskProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final DocumentTaskProperties documentTaskProperties;

    /**
     * Publishes a parse task to the main parse exchange.
     *
     * This method intentionally catches and logs all exceptions. Upload success should
     * not be rolled back because RabbitMQ is temporarily unavailable; a later
     * compensation process can scan UPLOAD_COMPLETED documents and resend messages.
     */
    public void send(DocumentParseMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    documentTaskProperties.getParseExchange(),
                    documentTaskProperties.getParseRoutingKey(),
                    message
            );
            log.info("Sent document parse message, documentId={}, retryCount={}, maxRetryCount={}",
                    message.documentId(), message.retryCount(), message.maxRetryCount());
        } catch (Exception ex) {
            log.error("Failed to send document parse message, documentId={}, retryCount={}",
                    message.documentId(), message.retryCount(), ex);
        }
    }
}
