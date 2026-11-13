package com.smartagent.agent.agent;

import com.alibaba.fastjson.JSON;
import com.smartagent.agent.entity.AgentType;
import com.smartagent.common.config.DashScopeAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

@Slf4j
@Component
public class SupervisorAgent {
    private final DashScopeAiClient aiClient;
    private final Map<AgentType, SpecialistAgent> specialistAgents = new EnumMap<>(AgentType.class);
    private final AgentRoutingProperties routing;

    public SupervisorAgent(DashScopeAiClient aiClient, List<SpecialistAgent> specialists, AgentRoutingProperties routing) {
        this.aiClient = aiClient;
        this.routing = routing;
        for (SpecialistAgent specialist : specialists) {
            if (specialistAgents.putIfAbsent(specialist.getType(), specialist) != null) {
                throw new IllegalArgumentException("Duplicate specialist agent type: " + specialist.getType());
            }
        }
    }

    public AgentResponse processMessage(String message, String sessionId, Long userId) {
        long started = System.currentTimeMillis();
        AgentResponse response = process(message, sessionId, userId);
        response.setSessionId(sessionId);
        response.setProcessingTimeMs(System.currentTimeMillis() - started);
        return response;
    }

    private AgentResponse process(String message, String sessionId, Long userId) {
        String raw;
        try {
            raw = aiClient.chat(buildClassificationPrompt(message));
        } catch (Exception e) {
            log.error("Intent model call failed for session {}", sessionId, e);
            return failure("MODEL_CALL_FAILED", "暂时无法连接智能服务，请稍后重试。");
        }
        IntentClassification classification;
        try {
            classification = parseClassification(raw);
        } catch (Exception e) {
            log.warn("Invalid intent classification for session {}", sessionId, e);
            return failure("CLASSIFICATION_PARSE_FAILED", "智能服务返回的分类结果无效，请稍后重试。");
        }
        AgentType intent = classification.intent;
        if (intent == AgentType.ESCALATION && classification.confidence >= routing.getMinConfidence()) {
            return escalate(intent, "MODEL_REQUESTED_ESCALATION", classification.reasoning,
                "您的问题需要人工协助处理。", null);
        }
        Optional<AgentRoutingProperties.Rule> rule = routing.match(intent, message);
        if (rule.isPresent()) {
            AgentRoutingProperties.Rule matched = rule.get();
            return escalate(intent, "BUSINESS_RULE_ESCALATION", matched.getReason(), matched.getMessage(), matched.getId());
        }
        if (classification.confidence < routing.getMinConfidence()) {
            return AgentResponse.builder().success(true).reasonCode("INTENT_UNCLEAR")
                .intent(intent).requiresEscalation(false)
                .message("我还不确定您的具体需求，请补充问题背景或您希望解决的事项。").build();
        }
        SpecialistAgent specialist = specialistAgents.get(intent);
        if (specialist == null) {
            return failure("SPECIALIST_UNAVAILABLE", "对应的智能助手暂不可用，请稍后重试。");
        }
        try {
            // Successful classification is authoritative: do not re-check message keywords.
            AgentResponse response = specialist.process(message, sessionId, userId);
            response.setIntent(intent);
            return response;
        } catch (Exception e) {
            log.error("Specialist processing failed for session {}", sessionId, e);
            return failure("SPECIALIST_PROCESSING_FAILED", "智能助手处理失败，请稍后重试。");
        }
    }

    private AgentResponse failure(String code, String message) {
        return AgentResponse.builder().success(false).reasonCode(code)
            .requiresEscalation(false).message(message).build();
    }

    private AgentResponse escalate(AgentType intent, String code, String reason, String message, String ruleId) {
        return AgentResponse.builder().success(true).reasonCode(code).requiresEscalation(true)
            .intent(intent).originalIntent(intent).escalationReason(reason).message(message)
            .metadata(ruleId == null ? Map.of() : Map.of("ruleId", ruleId)).build();
    }

    private String buildClassificationPrompt(String message) {
        return String.format("""
            You are a customer service intent classifier. Analyze the customer's message and classify it into one of these categories:
            
            - PRODUCT: Questions about products, features, specifications, pricing, availability
            - COMPLAINT: Customer dissatisfaction, negative feedback, service complaints
            - REFUND: Requests for refunds, money back, cancellation with refund
            - ORDER: Order status, modifications, cancellations (without refund), shipping
            - TECHNICAL: Technical support, troubleshooting, how-to questions
            - BILLING: Payment issues, invoices, charges, subscriptions
            - ESCALATION: The customer explicitly requests a human, or human intervention is clearly necessary

            Do not choose ESCALATION merely because the message is vague, emotional, or covers multiple topics.
            When intent is unclear, choose the closest category with confidence below 0.6 so we can ask for clarification.
            
            Customer Message: "%s"
            
            Respond ONLY with a JSON object in this exact format (no other text):
            {"intent": "CATEGORY", "confidence": 0.0-1.0, "reasoning": "brief explanation"}
            
            Examples:
            - "Where's my order #12345?" -> {"intent": "ORDER", "confidence": 0.95, "reasoning": "Asking about order status"}
            - "This product is terrible, I want my money back" -> {"intent": "REFUND", "confidence": 0.9, "reasoning": "Requesting refund"}
            - "How do I reset my password?" -> {"intent": "TECHNICAL", "confidence": 0.9, "reasoning": "Technical support request"}
            """, message);
    }

    private IntentClassification parseClassification(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Empty classification");
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        Map<String, Object> parsed = JSON.parseObject(cleaned);
        if (parsed == null || !(parsed.get("intent") instanceof String name)
                || !(parsed.get("confidence") instanceof Number number)) {
            throw new IllegalArgumentException("Missing intent or numeric confidence");
        }
        AgentType intent = AgentType.valueOf(name.trim().toUpperCase(Locale.ROOT));
        double confidence = number.doubleValue();
        if (intent == AgentType.SUPERVISOR || !Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("Invalid intent or confidence");
        }
        return new IntentClassification(intent, confidence, Objects.toString(parsed.get("reasoning"), ""));
    }

    private record IntentClassification(AgentType intent, double confidence, String reasoning) {}

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgentResponse {
        private boolean success;
        private String reasonCode;
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
