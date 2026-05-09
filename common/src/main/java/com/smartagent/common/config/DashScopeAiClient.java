package com.smartagent.common.config;

import com.alibaba.dashscope.aigc.messages.*;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.ApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Alibaba Cloud DashScope AI SDK client for LLM integration
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

    /**
     * Call LLM with messages
     */
    public String chat(String prompt) {
        return chat(prompt, defaultModel, defaultTemperature, defaultMaxTokens);
    }

    /**
     * Call LLM with custom parameters
     */
    public String chat(String prompt, String model, Double temperature, Integer maxTokens) {
        try {
            MultiMessage messages = MultiMessage.builder()
                    .add(UserMessage.from(prompt))
                    .build();

            ApiResult result = callApi(model, messages, temperature, maxTokens);
            return extractContent(result);
        } catch (NoApiKeyException e) {
            log.error("No API key configured for DashScope");
            throw new RuntimeException("AI service not configured properly", e);
        } catch (ApiException e) {
            log.error("DashScope API error: {}", e.getMessage());
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    /**
     * Chat with system prompt and user message
     */
    public String chatWithSystem(String systemPrompt, String userMessage) {
        return chatWithSystem(systemPrompt, userMessage, defaultModel, defaultTemperature, defaultMaxTokens);
    }

    /**
     * Chat with system prompt and user message with custom parameters
     */
    public String chatWithSystem(String systemPrompt, String userMessage, String model, 
                                  Double temperature, Integer maxTokens) {
        try {
            MultiMessage messages = MultiMessage.builder()
                    .add(SystemMessage.from(systemPrompt))
                    .add(UserMessage.from(userMessage))
                    .build();

            ApiResult result = callApi(model, messages, temperature, maxTokens);
            return extractContent(result);
        } catch (NoApiKeyException e) {
            log.error("No API key configured for DashScope");
            throw new RuntimeException("AI service not configured properly", e);
        } catch (ApiException e) {
            log.error("DashScope API error: {}", e.getMessage());
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    /**
     * Classify intent using LLM
     */
    public String classifyIntent(String message, String[] categories) {
        String categoryList = String.join(", ", categories);
        String prompt = String.format(
            "Classify the following customer message into one of these categories: %s\n\n" +
            "Message: %s\n\n" +
            "Respond with ONLY the category name, nothing else.",
            categoryList, message
        );
        return chat(prompt).trim();
    }

    /**
     * Generate embedding for text
     */
    public float[] generateEmbedding(String text) {
        // Placeholder - in production, use the embeddings API
        log.info("Generating embedding for text of length: {}", text.length());
        // Return a mock embedding for compilation
        return new float[1536];
    }

    private ApiResult callApi(String model, MultiMessage messages, 
                               Double temperature, Integer maxTokens) throws NoApiKeyException, ApiException {
        GenerationParam param = GenerationParam.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .resultFormat(ResultFormat.MESSAGE)
                .build();

        Generation generation = new Generation();
        return generation.call(param);
    }

    private String extractContent(ApiResult result) {
        if (result != null && result.getOutput() != null) {
            return result.getOutput().getChoices().getMessage().getContent();
        }
        return "";
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
