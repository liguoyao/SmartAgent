package com.smartagent.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name = "SmartAgent AI Service";
    private String version = "1.0.0";
    private boolean debug = false;

    // DeepSeek LLM configuration
    private LlmConfig deepseek = new LlmConfig();
    
    // Fallback LLM configuration
    private LlmConfig fallback = new LlmConfig();

    // Vector store configuration
    private VectorStoreConfig vectorStore = new VectorStoreConfig();

    // Redis configuration
    private RedisConfig redis = new RedisConfig();

    // Retry settings
    private int maxRetries = 3;

    @Data
    public static class LlmConfig {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com/v1";
        private String model = "deepseek-chat";
        private int maxTokens = 2048;
        private double temperature = 0.7;
    }

    @Data
    public static class VectorStoreConfig {
        private String path = "./data/vector_store";
        private String embeddingModel = "sentence-transformers/all-MiniLM-L6-v2";
        private int topK = 5;
    }

    @Data
    public static class RedisConfig {
        private String host = "localhost";
        private int port = 6379;
        private int db = 0;
        private String password;
        private int conversationTtl = 3600;
    }
}