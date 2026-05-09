package com.smartagent.agent.agent;

import com.alibaba.fastjson.JSON;
import com.smartagent.agent.entity.Agent;
import com.smartagent.agent.entity.AgentConversationContext;
import com.smartagent.agent.entity.AgentType;
import com.smartagent.agent.service.AgentService;
import com.smartagent.common.config.DashScopeAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Supervisor Agent - Main orchestrator that:
 * 1. Receives customer message
 * 2. Classifies intent using LLM
 * 3. Routes to appropriate Specialist Agent
 * 4. Collects response or escalates to human
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupervisorAgent {

    private final DashScopeAiClient aiClient;
    private final AgentService agentService;
    private final Map<AgentType, SpecialistAgent> specialistAgents;

    private static final String[] INTENT_CATEGORIES = {
            "PRODUCT", "COMPLAINT", "REFUND", "ORDER", "TECHNICAL", "BILLING", "ESCALATION"
    };

    /**
     * Process customer message through the supervisor agent
     */
    public AgentResponse processMessage(String message, String sessionId, Long userId) {
        log.info("SupervisorAgent processing message for session: {}, user: {}", sessionId, userId);
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Classify intent using LLM
            IntentClassification classification = classifyIntent(message);
            log.info("Intent classified as: {} with confidence: {}", 
                    classification.intent, classification.confidence);

            // Step 2: Route to appropriate specialist agent
            if (classification.intent == AgentType.ESCALATION || classification.confidence < 0.6) {
                return escalateToHuman(message, sessionId, userId, classification);
            }

            // Step 3: Get specialist agent and process
            AgentResponse response = routeToSpecialist(
                    message, 
                    classification.intent, 
                    sessionId, 
                    userId
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("SupervisorAgent processed message in {}ms, routed to {}", duration, classification.intent);
            
            response.setProcessingTimeMs(duration);
            response.setIntent(classification.intent);
            return response;

        } catch (Exception e) {
            log.error("Error processing message in SupervisorAgent", e);
            return AgentResponse.builder()
                    .success(false)
                    .message("I apologize, but I encountered an error processing your request. " +
                            "Let me connect you with a human agent who can better assist you.")
                    .requiresEscalation(true)
                    .build();
        }
    }

    /**
     * Classify customer intent using LLM
     */
    private IntentClassification classifyIntent(String message) {
        String classificationPrompt = buildClassificationPrompt(message);
        
        try {
            String llmResponse = aiClient.chat(classificationPrompt);
            return parseClassification(llmResponse, message);
        } catch (Exception e) {
            log.error("Error classifying intent, defaulting to ESCALATION", e);
            return new IntentClassification(AgentType.ESCALATION, 0.0, "Classification failed");
        }
    }

    /**
     * Build prompt for intent classification
     */
    private String buildClassificationPrompt(String message) {
        return String.format("""
            You are a customer service intent classifier. Analyze the customer's message and classify it into one of these categories:
            
            - PRODUCT: Questions about products, features, specifications, pricing, availability
            - COMPLAINT: Customer dissatisfaction, negative feedback, service complaints
            - REFUND: Requests for refunds, money back, cancellation with refund
            - ORDER: Order status, modifications, cancellations (without refund), shipping
            - TECHNICAL: Technical support, troubleshooting, how-to questions
            - BILLING: Payment issues, invoices, charges, subscriptions
            - ESCALATION: Complex issues, emotional customers, multiple topics, human needed
            
            Customer Message: "%s"
            
            Respond ONLY with a JSON object in this exact format (no other text):
            {"intent": "CATEGORY", "confidence": 0.0-1.0, "reasoning": "brief explanation"}
            
            Examples:
            - "Where's my order #12345?" -> {"intent": "ORDER", "confidence": 0.95, "reasoning": "Asking about order status"}
            - "This product is terrible, I want my money back" -> {"intent": "REFUND", "confidence": 0.9, "reasoning": "Requesting refund"}
            - "How do I reset my password?" -> {"intent": "TECHNICAL", "confidence": 0.9, "reasoning": "Technical support request"}
            """, message);
    }

    /**
     * Parse LLM classification response
     */
    private IntentClassification parseClassification(String llmResponse, String message) {
        try {
            // Clean the response
            String cleanedResponse = llmResponse.trim();
            if (cleanedResponse.contains("```json")) {
                cleanedResponse = cleanedResponse.substring(cleanedResponse.indexOf("```json") + 7);
                cleanedResponse = cleanedResponse.replace("```", "").trim();
            } else if (cleanedResponse.contains("```")) {
                cleanedResponse = cleanedResponse.replace("```", "").trim();
            }

            Map<String, Object> parsed = JSON.parseObject(cleanedResponse);
            
            String intentStr = parsed.get("intent").toString().toUpperCase();
            AgentType intent = AgentType.valueOf(intentStr);
            double confidence = ((Number) parsed.get("confidence")).doubleValue();
            String reasoning = parsed.get("reasoning") != null ? parsed.get("reasoning").toString() : "";

            return new IntentClassification(intent, confidence, reasoning);
        } catch (Exception e) {
            log.error("Failed to parse classification response: {}", llmResponse, e);
            // Fallback: try keyword matching
            return fallbackClassification(message);
        }
    }

    /**
     * Fallback classification using keyword matching
     */
    private IntentClassification fallbackClassification(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (containsAny(lowerMessage, "refund", "money back", "return", "cancel order")) {
            return new IntentClassification(AgentType.REFUND, 0.5, "Keyword match");
        } else if (containsAny(lowerMessage, "order", "shipping", "delivery", "track")) {
            return new IntentClassification(AgentType.ORDER, 0.5, "Keyword match");
        } else if (containsAny(lowerMessage, "complaint", "terrible", "angry", "frustrated", "disappointed")) {
            return new IntentClassification(AgentType.COMPLAINT, 0.5, "Keyword match");
        } else if (containsAny(lowerMessage, "technical", "error", "bug", "not working", "how to")) {
            return new IntentClassification(AgentType.TECHNICAL, 0.5, "Keyword match");
        } else if (containsAny(lowerMessage, "price", "cost", "payment", "bill", "invoice")) {
            return new IntentClassification(AgentType.BILLING, 0.5, "Keyword match");
        } else if (containsAny(lowerMessage, "product", "feature", "spec", "available")) {
            return new IntentClassification(AgentType.PRODUCT, 0.5, "Keyword match");
        }
        
        return new IntentClassification(AgentType.ESCALATION, 0.3, "No clear intent matched");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Route message to the appropriate specialist agent
     */
    private AgentResponse routeToSpecialist(String message, AgentType intent, 
                                            String sessionId, Long userId) {
        SpecialistAgent specialist = specialistAgents.get(intent);
        
        if (specialist == null) {
            log.warn("No specialist agent found for intent: {}", intent);
            return escalateToHuman(message, sessionId, userId, 
                    new IntentClassification(intent, 0.0, "No specialist available"));
        }

        // Check if specialist can handle the request
        if (!specialist.canHandle(message)) {
            log.info("Specialist {} cannot fully handle the request, escalating", intent);
            return escalateToHuman(message, sessionId, userId, 
                    new IntentClassification(intent, 0.0, "Request too complex"));
        }

        // Process with specialist
        return specialist.process(message, sessionId, userId);
    }

    /**
     * Escalate to human agent
     */
    private AgentResponse escalateToHuman(String message, String sessionId, 
                                          Long userId, IntentClassification classification) {
        log.info("Escalating to human agent - Session: {}, Reason: {}", 
                sessionId, classification.reasoning);
        
        return AgentResponse.builder()
                .success(true)
                .message("I understand this is a complex matter that requires personalized attention. " +
                        "I'm connecting you with a human agent who can better assist you. " +
                        "Please hold for a moment.")
                .requiresEscalation(true)
                .escalationReason(classification.reasoning)
                .originalIntent(classification.intent)
                .sessionId(sessionId)
                .build();
    }

    /**
     * Intent classification result
     */
    private record IntentClassification(AgentType intent, double confidence, String reasoning) {}

    /**
     * Agent response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgentResponse {
        private boolean success;
        private String message;
        private AgentType intent;
        private AgentType routedTo;
        private boolean requiresEscalation;
        private String escalationReason;
        private AgentType originalIntent;
        private String sessionId;
        private Long processingTimeMs;
        private Map<String, Object> metadata;
    }
}
