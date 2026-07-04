package com.bank.aiassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置。
 *
 * 检索日志写入、BM25/向量两路召回并发执行都使用独立线程池，避免占用 Web 容器线程。
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean("retrievalExecutor")
    public Executor retrievalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("retrieval-");
        executor.initialize();
        return executor;
    }
}
