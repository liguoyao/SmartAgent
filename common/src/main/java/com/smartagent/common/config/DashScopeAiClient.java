package com.smartagent.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Alibaba Cloud DashScope AI SDK client for LLM integration
 * Uses HTTP API directly for better compatibility
 */
@Slf4j
@Component
public class DashScopeAiClient {

    private final String apiKey;

    @Value("${dashscope.model:qwen-turbo}")
    private String defaultModel;

    @Value("${dashscope.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${dashscope.max-tokens:2000}")
    private Integer defaultMaxTokens;

    private final WebClient webClient;
    private final boolean compatibleMode;

    public DashScopeAiClient(WebClient.Builder webClientBuilder,
            @Value("${dashscope.api-key:}") String apiKey,
            @Value("${dashscope.base-url:https://dashscope.aliyuncs.com/api/v1}") String baseUrl) {
        this.apiKey = apiKey;
        String normalizedBaseUrl = baseUrl.replaceAll("/+$", "");
        this.compatibleMode = java.net.URI.create(normalizedBaseUrl).getPath().endsWith("/compatible-mode/v1");
        this.webClient = webClientBuilder
            .baseUrl(normalizedBaseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }

    public String chat(String prompt) {
        return chat(prompt, defaultModel, defaultTemperature, defaultMaxTokens);
    }

    public String chat(String prompt, String model, Double temperature, Integer maxTokens) {
        try {
            return sendChat(List.of(Map.of("role", "user", "content", prompt)), model, temperature, maxTokens);
        } catch (Exception e) {
            log.error("DashScope API error: {}", e.getMessage());
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    public String chatWithSystem(String systemPrompt, String userMessage) {
        return chatWithSystem(systemPrompt, userMessage, defaultModel, defaultTemperature, defaultMaxTokens);
    }

    public String chatWithSystem(String systemPrompt, String userMessage, String model, 
                                 Double temperature, Integer maxTokens) {
        try {
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            );
            return sendChat(messages, model, temperature, maxTokens);
        } catch (Exception e) {
            log.error("DashScope API error: {}", e.getMessage());
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    public String classifyIntent(String message, String[] categories) {
        String categoryList = String.join(", ", categories);
        String prompt = String.format(
            "Classify the following customer message into one of these categories: %s\n\n" +
            "Message: %s\n\nRespond with ONLY the category name.",
            categoryList, message
        );
        return chat(prompt).trim();
    }

    public float[] generateEmbedding(String text) {
        try {
            Map<String, Object> request = Map.of(
                "model", "text-embedding-v3",
                "input", Map.of("texts", List.of(text))
            );
            Map<String, Object> response = webClient.post()
                .uri("/services/embeddings/text-embedding/text-embedding")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) response.get("output");
                List<?> embeddings = (List<?>) output.get("embeddings");
                if (embeddings != null && !embeddings.isEmpty()) {
                    Object first = embeddings.get(0);
                    if (first instanceof List) {
                        List<?> vec = (List<?>) first;
                        float[] result = new float[vec.size()];
                        for (int i = 0; i < vec.size(); i++) {
                            result[i] = ((Number) vec.get(i)).floatValue();
                        }
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Embedding generation failed: {}", e.getMessage());
        }
        return new float[1536];
    }

    private String sendChat(List<Map<String, String>> messages, String model, Double temperature, Integer maxTokens) {
        Map<String, Object> response = webClient.post()
            .uri(compatibleMode ? "/chat/completions" : "/services/aigc/text-generation/generation")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(buildRequest(messages, model, temperature, maxTokens))
            .retrieve()
            .bodyToMono(Map.class)
            .block();
        return extractContent(response);
    }

    private Map<String, Object> buildRequest(List<Map<String, String>> messages, String model, Double temperature, Integer maxTokens) {
        if (compatibleMode) {
            return Map.of(
                "model", model,
                "messages", messages,
                "temperature", temperature,
                "max_tokens", maxTokens
            );
        }
        return Map.of(
            "model", model,
            "input", Map.of("messages", messages),
            "parameters", Map.of(
                "temperature", temperature,
                "max_tokens", maxTokens,
                "result_format", "message"
            )
        );
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (compatibleMode && response != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null && message.get("content") instanceof String content) {
                    return content;
                }
            }
            throw new IllegalStateException("Chat completion response is missing choices[0].message.content");
        }
        if (response != null && response.containsKey("output")) {
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            if (output.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            if (output.containsKey("text")) {
                return (String) output.get("text");
            }
        }
        return "";
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
