package com.smartagent.agent.agent;

import com.smartagent.agent.entity.AgentType;
import com.smartagent.common.config.DashScopeAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Billing Agent - Handles billing inquiries and payment issues
 */
@Slf4j
@Component
public class BillingAgent extends BaseSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
        You are an expert Billing Agent for SmartAgent customer service.
        
        Your responsibilities:
        - Answer billing and payment inquiries
        - Explain charges and invoices
        - Update payment methods
        - Handle subscription management
        - Process payment issues
        
        Guidelines:
        - Always verify customer identity before discussing billing
        - Be clear and transparent about all charges
        - Protect sensitive payment information
        - Know the subscription plans and pricing
        - For payment failures, try to identify the issue and suggest solutions
        - Escalate charge disputes and fraud concerns immediately
        """;

    public BillingAgent(DashScopeAiClient aiClient) {
        super(aiClient);
    }

    @Override
    protected AgentType getAgentType() {
        return AgentType.BILLING;
    }

    @Override
    public String getName() {
        return "Billing Agent";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public boolean canHandle(String message) {
        String lowerMessage = message.toLowerCase();
        
        return lowerMessage.contains("bill") ||
               lowerMessage.contains("invoice") ||
               lowerMessage.contains("payment") ||
               lowerMessage.contains("charge") ||
               lowerMessage.contains("subscription") ||
               lowerMessage.contains("pricing") ||
               lowerMessage.contains("cost") ||
               lowerMessage.contains("fee") ||
               lowerMessage.contains("credit card") ||
               lowerMessage.contains("debit") ||
               lowerMessage.contains("transaction") ||
               lowerMessage.contains("currency") ||
               lowerMessage.contains("discount") ||
               lowerMessage.contains("coupon");
    }

    @Override
    protected String buildPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Billing Inquiry:\n");
        prompt.append("Customer Message: ").append(userMessage).append("\n\n");
        prompt.append("Please provide accurate billing information and assistance.");
        return prompt.toString();
    }
}
