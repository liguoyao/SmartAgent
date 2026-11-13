package com.smartagent.agent.agent;

import com.smartagent.agent.entity.AgentType;
import com.smartagent.common.config.DashScopeAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Complaint Agent - Handles customer complaints and negative feedback
 */
@Slf4j
@Component
public class ComplaintAgent extends BaseSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
        You are an expert Complaint Agent for SmartAgent customer service.
        
        Your responsibilities:
        - Acknowledge and validate customer complaints empathetically
        - Listen actively and show understanding of the customer's frustration
        - Gather information about the complaint details
        - Propose solutions or escalate appropriately
        - Follow up to ensure satisfaction
        
        Guidelines:
        - Always acknowledge the customer's feelings first
        - Never blame the customer or make excuses
        - Take ownership of the issue
        - Provide timely resolution or clear next steps
        - Maintain calm and professional tone even with upset customers
        - If the issue cannot be resolved, escalate to a human agent
        """;

    public ComplaintAgent(DashScopeAiClient aiClient) {
        super(aiClient);
    }

    @Override
    protected AgentType getAgentType() {
        return AgentType.COMPLAINT;
    }

    @Override
    public String getName() {
        return "Complaint Agent";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public boolean canHandle(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Complaint indicators
        return lowerMessage.contains("complaint") ||
               lowerMessage.contains("unhappy") ||
               lowerMessage.contains("disappointed") ||
               lowerMessage.contains("frustrated") ||
               lowerMessage.contains("angry") ||
               lowerMessage.contains("terrible") ||
               lowerMessage.contains("worst") ||
               lowerMessage.contains("horrible") ||
               lowerMessage.contains("not satisfied") ||
               lowerMessage.contains("feedback") ||
               (lowerMessage.contains("bad") && lowerMessage.contains("experience")) ||
               lowerMessage.contains("problem") && !lowerMessage.contains("solve");
    }

    @Override
    protected String buildPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Customer Complaint: ").append(userMessage).append("\n\n");
        prompt.append("Please respond with empathy and understanding. ");
        prompt.append("Acknowledge their frustration, apologize for the experience, ");
        prompt.append("and provide a clear path to resolution.");
        return prompt.toString();
    }
}
