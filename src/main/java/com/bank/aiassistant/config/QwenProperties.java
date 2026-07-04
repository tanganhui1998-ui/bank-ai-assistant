package com.bank.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "qwen")
public class QwenProperties {

    private String apiKey;
    private String baseUrl;
    private Chat chat = new Chat();
    private Embedding embedding = new Embedding();

    @Getter
    @Setter
    public static class Chat {
        private String model = "qwen-plus";
        private Double temperature = 0.2;
        private Integer timeoutMillis = 5000;
        private Integer intentTimeoutMillis = 450;
    }

    @Getter
    @Setter
    public static class Embedding {
        private String model = "text-embedding-v2";
        private Integer maxBatchSize = 100;
        private Integer maxRetries = 3;
        private Integer maxInputChars = 3500;
    }
}
