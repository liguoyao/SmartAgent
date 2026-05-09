package com.smartagent.aiservice.service;

import com.smartagent.aiservice.config.AppProperties;
import com.smartagent.aiservice.exception.LLMException;
import com.smartagent.aiservice.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LLMClientService {

    private final AppProperties appProperties;
    private WebClient webClient;

    public LLMClientService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Retryable(
        retryFor = {LLMException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        return chatWithModel(messages, tools, appProperties.getDeepseek().getModel(), 
            appProperties.getDeepseek().getApiKey(), appProperties.getDeepseek().getBaseUrl());
    }

    public Map<String, Object> chatWithFallback(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        try {
            return chat(messages, tools);
        } catch (RateLimitException e) {
            log.warn("Primary LLM rate limited, trying fallback");
            if (appProperties.getFallback().getApiKey() != null) {
                return chatWithModel(messages, tools, appProperties.getFallback().getModel(),
                    appProperties.getFallback().getApiKey(), appProperties.getFallback().getBaseUrl());
            }
            throw e;
        }
    }

    private Map<String, Object> chatWithModel(
            List<Map<String, Object>> messages, 
            List<Map<String, Object>> tools,
            String model, String apiKey, String baseUrl) {
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new LLMException("API key not configured");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", appProperties.getDeepseek().getTemperature());
        payload.put("max_tokens", appProperties.getDeepseek().getMaxTokens());
        payload.put("stream", false);

        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", tools);
            payload.put("tool_choice", "auto");
        }

        String url = baseUrl + "/chat/completions";

        try {
            Map<String, Object> response = webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(this::isRetriableError))
                .block();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response;
            return result;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new RateLimitException("Rate limit exceeded for model " + model);
            }
            throw new LLMException("HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new LLMException("LLM request failed: " + e.getMessage(), e);
        }
    }

    public String chatSimple(String prompt, String systemPrompt) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (systemPrompt != null) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", prompt));
        
        Map<String, Object> response = chat(messages, null);
        return extractContent(response);
    }

    public String extractContent(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            return "";
        }
        
        return (String) message.get("content");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> extractToolCalls(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            return Collections.emptyList();
        }
        
        return (List<Map<String, Object>>) message.get("tool_calls");
    }

    private boolean isRetriableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
        }
        return throwable instanceof java.net.ConnectException || 
               throwable instanceof java.net.SocketTimeoutException;
    }
}