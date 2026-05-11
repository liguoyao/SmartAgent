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

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.base-url:https://dashscope.aliyuncs.com/api/v1}")
    private String baseUrl;

    @Value("${dashscope.model:qwen-turbo}")
    private String defaultModel;

    @Value("${dashscope.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${dashscope.max-tokens:2000}")
    private Integer defaultMaxTokens;

    private final WebClient webClient;

    public DashScopeAiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }

    public String chat(String prompt) {
        return chat(prompt, defaultModel, defaultTemperature, defaultMaxTokens);
    }

    public String chat(String prompt, String model, Double temperature, Integer maxTokens) {
        try {
            Map<String, Object> request = buildRequest(prompt, model, temperature, maxTokens);
            Map<String, Object> response = webClient.post()
                .uri("/services/aigc/text-generation/generation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            return extractContent(response);
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
            Map<String, Object> request = Map.of(
                "model", model,
                "input", Map.of("messages", messages),
                "parameters", Map.of(
                    "temperature", temperature,
                    "max_tokens", maxTokens,
                    "result_format", "message"
                )
            );
            Map<String, Object> response = webClient.post()
                .uri("/services/aigc/text-generation/generation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            return extractContent(response);
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

    private Map<String, Object> buildRequest(String prompt, String model, Double temperature, Integer maxTokens) {
        return Map.of(
            "model", model,
            "input", Map.of("messages", List.of(
                Map.of("role", "user", "content", prompt)
            )),
            "parameters", Map.of(
                "temperature", temperature,
                "max_tokens", maxTokens,
                "result_format", "message"
            )
        );
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
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
