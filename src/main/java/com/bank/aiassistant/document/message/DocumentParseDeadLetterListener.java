package com.bank.aiassistant.document.message;

import com.bank.aiassistant.document.parse.DocumentParseWorkflowService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseDeadLetterListener {

    private final DocumentParseWorkflowService workflowService;

    /**
     * Consumes permanently failed parse messages from the dead-letter queue.
     *
     * The current implementation records alert logs and marks the document as FAILED.
     * A later notification adapter can replace the log-only alert with email, SMS,
     * incident platform, or operations dashboard integration.
     */
    @RabbitListener(queues = "${app.ai.document.parse-dead-letter-queue}")
    public void onDeadLetterMessage(
            DocumentParseMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.error("Document parse message entered dead letter queue, documentId={}, retryCount={}, maxRetryCount={}",
                message.documentId(), message.retryCount(), message.maxRetryCount());
        workflowService.markFailed(message.documentId(), safeRetryCount(message.retryCount()), "Message entered dead letter queue");
        log.error("ALERT document parse failed permanently, documentId={}, operatorId={}, operatorName={}",
                message.documentId(), message.operatorId(), message.operatorName());
        // ACK the DLQ message after recording failure so it does not loop forever.
        channel.basicAck(deliveryTag, false);
        log.info("Acked document parse dead letter message, documentId={}", message.documentId());
    }

    private int safeRetryCount(Integer retryCount) {
        return retryCount == null ? 0 : retryCount;
    }
}
