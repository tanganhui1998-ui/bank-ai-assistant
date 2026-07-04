package com.bank.aiassistant.document.message;

import com.bank.aiassistant.document.parse.DocumentParseResult;
import com.bank.aiassistant.document.parse.DocumentParseWorkflowService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseMessageListener {

    private final DocumentParseWorkflowService workflowService;
    private final DocumentParseMessageProducer messageProducer;

    /**
     * Consumes document parse tasks from the main queue.
     *
     * Manual ACK is used so the application, not the listener container, decides
     * exactly when RabbitMQ can remove a message. The method ACKs only after one of
     * these outcomes:
     * - parsing completed successfully;
     * - the message is idempotently skipped;
     * - a retry message has been published.
     *
     * When retry limit is reached, the method NACKs with requeue=false so RabbitMQ
     * routes the original message to the configured dead-letter queue.
     */
    @RabbitListener(queues = "${app.ai.document.parse-queue}")
    public void onMessage(
            DocumentParseMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.info("Received document parse message, documentId={}, retryCount={}, maxRetryCount={}",
                message.documentId(), message.retryCount(), message.maxRetryCount());

        try {
            DocumentParseResult result = workflowService.parse(message);
            // ACK after the transactional workflow returns. The message is now safe to remove.
            channel.basicAck(deliveryTag, false);
            log.info("Acked document parse message, documentId={}, result={}", message.documentId(), result);
        } catch (Exception ex) {
            handleParseFailure(message, channel, deliveryTag, ex);
        }
    }

    private void handleParseFailure(
            DocumentParseMessage message,
            Channel channel,
            long deliveryTag,
            Exception ex
    ) throws IOException {
        int retryCount = safeRetryCount(message.retryCount());
        int maxRetryCount = safeMaxRetryCount(message.maxRetryCount());
        log.error("Document parse failed, documentId={}, retryCount={}, maxRetryCount={}",
                message.documentId(), retryCount, maxRetryCount, ex);

        if (retryCount < maxRetryCount) {
            // RabbitMQ cannot update retryCount on an existing message. Publish a new
            // message carrying retryCount + 1, then ACK the current failed delivery.
            DocumentParseMessage retryMessage = new DocumentParseMessage(
                    message.documentId(),
                    message.tenantId(),
                    message.operatorId(),
                    message.operatorName(),
                    LocalDateTime.now(),
                    retryCount + 1,
                    maxRetryCount
            );
            messageProducer.send(retryMessage);
            channel.basicAck(deliveryTag, false);
            log.info("Requeued document parse by publishing retry message, documentId={}, nextRetryCount={}",
                    message.documentId(), retryCount + 1);
            return;
        }

        // Retry budget exhausted. Mark the document failed, then reject the message
        // without requeue so it is routed to DLX/DLQ for operational inspection.
        workflowService.markFailed(message.documentId(), retryCount, buildErrorDetail(ex));
        channel.basicNack(deliveryTag, false, false);
        log.warn("Nacked document parse message to dead letter queue, documentId={}, retryCount={}",
                message.documentId(), retryCount);
    }

    private int safeRetryCount(Integer retryCount) {
        return retryCount == null ? 0 : retryCount;
    }

    /**
     * Keeps backward compatibility if old messages do not contain maxRetryCount.
     */
    private int safeMaxRetryCount(Integer maxRetryCount) {
        return maxRetryCount == null ? 3 : maxRetryCount;
    }

    /**
     * 生成可入库的异常摘要。完整堆栈已经写入日志，这里保留前 3000 字符，
     * 方便运营后台直接看到失败原因和关键调用链。
     */
    private String buildErrorDetail(Exception ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        String detail = writer.toString();
        return detail.length() <= 3000 ? detail : detail.substring(0, 3000);
    }
}
