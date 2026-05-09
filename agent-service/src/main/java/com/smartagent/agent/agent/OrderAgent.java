package com.smartagent.agent.agent;

import com.smartagent.agent.entity.AgentType;
import com.smartagent.common.config.DashScopeAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Order Agent - Handles order management inquiries
 */
@Slf4j
@Component
public class OrderAgent extends BaseSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
        You are an expert Order Agent for SmartAgent customer service.
        
        Your responsibilities:
        - Provide order status and tracking information
        - Handle order modifications (within allowed limits)
        - Process order cancellations (without refund - use Refund Agent for that)
        - Update shipping addresses
        - Handle delivery issues
        
        Guidelines:
        - Always verify order ownership before providing information
        - Know the cutoff times for order modifications
        - Be clear about what can and cannot be modified
        - For cancellation with refund, redirect to Refund Agent
        - Provide tracking numbers and carrier information when available
        """;

    public OrderAgent(DashScopeAiClient aiClient) {
        super(aiClient);
    }

    @Override
    protected AgentType getAgentType() {
        return AgentType.ORDER;
    }

    @Override
    public String getName() {
        return "Order Agent";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public boolean canHandle(String message) {
        String lowerMessage = message.toLowerCase();
        
        return lowerMessage.contains("order") ||
               lowerMessage.contains("track") ||
               lowerMessage.contains("shipping") ||
               lowerMessage.contains("delivery") ||
               lowerMessage.contains("package") ||
               lowerMessage.contains("status") ||
               lowerMessage.contains("#") && containsDigits(message);
    }

    private boolean containsDigits(String text) {
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String buildPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Order Inquiry:\n");
        prompt.append("Customer Message: ").append(userMessage).append("\n\n");
        
        // Extract order ID if present
        String orderId = extractOrderId(userMessage);
        if (orderId != null) {
            prompt.append("Order ID detected: ").append(orderId).append("\n");
        }
        
        prompt.append("Please provide accurate order information and tracking details.");
        return prompt.toString();
    }

    private String extractOrderId(String message) {
        // Simple order ID extraction - looks for patterns like #12345 or order 12345
        String[] parts = message.split("\\s+");
        for (String part : parts) {
            if (part.startsWith("#") && part.length() > 1) {
                return part.substring(1);
            }
            if (part.toLowerCase().startsWith("order") && part.length() > 5) {
                return part.substring(5);
            }
            if (part.matches("\\d{6,}")) {
                return part;
            }
        }
        return null;
    }
}
