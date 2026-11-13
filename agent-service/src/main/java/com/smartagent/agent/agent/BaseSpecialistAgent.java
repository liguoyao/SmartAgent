package com.smartagent.agent.agent;

import com.alibaba.fastjson.JSON;
import com.smartagent.common.config.DashScopeAiClient;
import com.smartagent.agent.entity.AgentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Base implementation for Specialist Agents with common functionality
 */
@Slf4j
public abstract class BaseSpecialistAgent implements SpecialistAgent {

    protected final DashScopeAiClient aiClient;
    
    @Value("${dashscope.model:qwen-turbo}")
    protected String model;

    @Value("${dashscope.temperature:0.7}")
    protected Double temperature;

    @Value("${dashscope.max-tokens:2000}")
    protected Integer maxTokens;

    protected BaseSpecialistAgent(DashScopeAiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public AgentType getType() {
        return getAgentType();
    }

    /**
     * Get the agent type - must be implemented by subclasses
     */
    protected abstract AgentType getAgentType();

    @Override
    public SupervisorAgent.AgentResponse process(String message, String sessionId, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            String response = generateResponse(message);
            long processingTime = System.currentTimeMillis() - startTime;
            
            return SupervisorAgent.AgentResponse.builder()
                    .success(true)
                    .message(response)
                    .intent(getType())
                    .routedTo(getType())
                    .requiresEscalation(false)
                    .sessionId(sessionId)
                    .processingTimeMs(processingTime)
                    .build();
        } catch (Exception e) {
            log.error("Error processing message with {}: {}", getName(), e.getMessage());
            return SupervisorAgent.AgentResponse.builder()
                    .success(false)
                    .message("智能助手暂时无法处理请求，请稍后重试。")
                    .reasonCode("SPECIALIST_PROCESSING_FAILED")
                    .requiresEscalation(false)
                    .originalIntent(getType())
                    .sessionId(sessionId)
                    .build();
        }
    }

    /**
     * Generate response using LLM with agent-specific prompt
     */
    protected String generateResponse(String userMessage) {
        String fullPrompt = buildPrompt(userMessage);
        return aiClient.chatWithSystem(getSystemPrompt(), fullPrompt);
    }

    /**
     * Build the full prompt with system prompt and user message
     */
    protected String buildPrompt(String userMessage) {
        return String.format("""
            Customer Message: %s
            
            Please provide a helpful, accurate, and professional response following your role guidelines.
            """, userMessage);
    }

    /**
     * Search knowledge base (placeholder - would integrate with knowledge-service)
     */
    protected String searchKnowledgeBase(String query) {
        // In production, this would call the knowledge-service via Feign
        // For now, return a placeholder
        log.info("Searching knowledge base for: {}", query);
        return null;
    }

    /**
     * Extract entities from message
     */
    protected Map<String, String> extractEntities(String message) {
        String extractionPrompt = String.format("""
            Extract the following entities from the customer message:
            - order_id: Any order number mentioned
            - product_id: Any product identifier
            - email: Any email address
            - phone: Any phone number
            - amount: Any monetary amount mentioned
            
            Message: %s
            
            Respond with ONLY a JSON object with these fields (null if not found):
            {"order_id": null, "product_id": null, "email": null, "phone": null, "amount": null}
            """, message);
        
        try {
            String response = aiClient.chat(extractionPrompt);
            return JSON.parseObject(response, Map.class);
        } catch (Exception e) {
            log.error("Error extracting entities", e);
            return new HashMap<>();
        }
    }

}
