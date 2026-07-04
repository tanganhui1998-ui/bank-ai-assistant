package com.bank.aiassistant.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DocumentTaskProperties.class)
public class RabbitConfig {

    /**
     * Main direct exchange for document parse tasks.
     *
     * Producers publish parse messages to this exchange with parseRoutingKey, and
     * RabbitMQ routes them to the durable parse queue below.
     */
    @Bean
    public DirectExchange documentParseExchange(DocumentTaskProperties properties) {
        return new DirectExchange(properties.getParseExchange(), true, false);
    }

    /**
     * Dead-letter exchange for parse messages that should no longer be retried.
     */
    @Bean
    public DirectExchange documentParseDeadLetterExchange(DocumentTaskProperties properties) {
        return new DirectExchange(properties.getParseDeadLetterExchange(), true, false);
    }

    /**
     * Main parse queue.
     *
     * Queue arguments:
     * - x-dead-letter-exchange: failed messages rejected with requeue=false are routed here.
     * - x-dead-letter-routing-key: routing key used for DLX delivery.
     * - x-message-ttl: task messages expire after the configured TTL, preventing very old
     *   parse tasks from being processed silently.
     */
    @Bean
    public Queue documentParseQueue(DocumentTaskProperties properties) {
        return QueueBuilder.durable(properties.getParseQueue())
                .deadLetterExchange(properties.getParseDeadLetterExchange())
                .deadLetterRoutingKey(properties.getParseDeadLetterRoutingKey())
                .ttl(Math.toIntExact(properties.getParseMessageTtlMillis()))
                .build();
    }

    /**
     * Dead-letter queue. Operators or compensating jobs can inspect this queue to
     * understand permanently failed parse tasks.
     */
    @Bean
    public Queue documentParseDeadLetterQueue(DocumentTaskProperties properties) {
        return QueueBuilder.durable(properties.getParseDeadLetterQueue()).build();
    }

    /**
     * Binds the main queue to the main exchange with the parse routing key.
     */
    @Bean
    public Binding documentParseBinding(
            @Qualifier("documentParseExchange") DirectExchange documentParseExchange,
            @Qualifier("documentParseQueue") Queue documentParseQueue,
            DocumentTaskProperties properties
    ) {
        return BindingBuilder.bind(documentParseQueue)
                .to(documentParseExchange)
                .with(properties.getParseRoutingKey());
    }

    /**
     * Binds the dead-letter queue to the DLX with the dead-letter routing key.
     */
    @Bean
    public Binding documentParseDeadLetterBinding(
            @Qualifier("documentParseDeadLetterExchange") DirectExchange documentParseDeadLetterExchange,
            @Qualifier("documentParseDeadLetterQueue") Queue documentParseDeadLetterQueue,
            DocumentTaskProperties properties
    ) {
        return BindingBuilder.bind(documentParseDeadLetterQueue)
                .to(documentParseDeadLetterExchange)
                .with(properties.getParseDeadLetterRoutingKey());
    }

    /**
     * Converts Java records/classes to JSON message bodies. This keeps message payloads
     * readable and forwards-compatible when more fields are added later.
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
