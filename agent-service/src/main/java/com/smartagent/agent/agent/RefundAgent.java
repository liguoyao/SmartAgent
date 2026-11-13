package com.smartagent.agent.agent;

import com.alibaba.fastjson.JSON;
import com.smartagent.agent.entity.AgentType;
import com.smartagent.common.config.DashScopeAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Refund Agent - Handles refund requests and processing
 */
@Slf4j
@Component
public class RefundAgent extends BaseSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
        You are an expert Refund Agent for SmartAgent customer service.
        
        Your responsibilities:
        - Process refund requests accurately and efficiently
        - Explain refund policies and timelines
        - Track refund status and provide updates
        - Handle cancellation requests with refund
        - Process partial refunds when applicable
        
        Guidelines:
        - Verify order and customer information
        - Clearly explain refund policies and expected timelines
        - Confirm refund amount and method
        - Provide refund tracking information
        - Escalate complex cases (chargebacks, disputes) to human agents
        - Be efficient as customers requesting refunds are often frustrated
        """;

    public RefundAgent(DashScopeAiClient aiClient) {
        super(aiClient);
    }

    @Override
    protected AgentType getAgentType() {
        return AgentType.REFUND;
    }

    @Override
    public String getName() {
        return "Refund Agent";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public boolean canHandle(String message) {
        String lowerMessage = message.toLowerCase();
        
        return lowerMessage.contains("refund") ||
               lowerMessage.contains("money back") ||
               lowerMessage.contains("cancel and refund") ||
               lowerMessage.contains("return item") && lowerMessage.contains("money") ||
               lowerMessage.contains("get my money back") ||
               lowerMessage.contains("reverse charge") ||
               lowerMessage.contains("chargeback");
    }

    @Override
    protected String buildPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Refund Request Details:\n");
        prompt.append("Customer Message: ").append(userMessage).append("\n\n");
        
        // Extract and include relevant order information
        Map<String, String> entities = extractEntities(userMessage);
        if (entities.get("order_id") != null) {
            prompt.append("Order ID mentioned: ").append(entities.get("order_id")).append("\n");
        }
        if (entities.get("amount") != null) {
            prompt.append("Amount mentioned: ").append(entities.get("amount")).append("\n");
        }
        
        prompt.append("\nPlease provide information about the refund process, ");
        prompt.append("including:\n");
        prompt.append("1. Confirmation of the order\n");
        prompt.append("2. Refund eligibility and amount\n");
        prompt.append("3. Expected timeline for processing\n");
        prompt.append("4. Next steps for the customer");
        
        return prompt.toString();
    }
}
