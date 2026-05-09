package com.smartagent.agent.agent;

import com.smartagent.agent.entity.AgentType;
import com.smartagent.common.config.DashScopeAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Technical Agent - Handles technical support and troubleshooting
 */
@Slf4j
@Component
public class TechnicalAgent extends BaseSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
        You are an expert Technical Support Agent for SmartAgent customer service.
        
        Your responsibilities:
        - Troubleshoot technical issues and errors
        - Guide customers through step-by-step solutions
        - Explain technical concepts in simple terms
        - Provide configuration and setup guidance
        - Handle account and access issues
        
        Guidelines:
        - Be patient and methodical in troubleshooting
        - Ask clarifying questions to understand the issue
        - Provide clear, step-by-step instructions
        - Use simple language, avoiding jargon
        - Include relevant screenshots or documentation links when helpful
        - For complex infrastructure issues, escalate to Tier 2 support
        """;

    public TechnicalAgent(DashScopeAiClient aiClient) {
        super(aiClient);
    }

    @Override
    protected AgentType getAgentType() {
        return AgentType.TECHNICAL;
    }

    @Override
    public String getName() {
        return "Technical Agent";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public boolean canHandle(String message) {
        String lowerMessage = message.toLowerCase();
        
        return lowerMessage.contains("error") ||
               lowerMessage.contains("not working") ||
               lowerMessage.contains("doesn't work") ||
               lowerMessage.contains("how to") ||
               lowerMessage.contains("setup") ||
               lowerMessage.contains("configure") ||
               lowerMessage.contains("install") ||
               lowerMessage.contains("password") ||
               lowerMessage.contains("login") ||
               lowerMessage.contains("access") ||
               lowerMessage.contains("account") ||
               lowerMessage.contains("technical") ||
               lowerMessage.contains("bug") ||
               lowerMessage.contains("issue") ||
               lowerMessage.contains("problem") ||
               lowerMessage.contains("troubleshoot") ||
               lowerMessage.contains("fix");
    }

    @Override
    protected String buildPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Technical Support Request:\n");
        prompt.append("Customer Message: ").append(userMessage).append("\n\n");
        
        // Search knowledge base for relevant solutions
        String knowledge = searchKnowledgeBase(userMessage);
        if (knowledge != null && !knowledge.isEmpty()) {
            prompt.append("Relevant Knowledge Base Articles:\n").append(knowledge).append("\n\n");
        }
        
        prompt.append("Please provide troubleshooting steps or a solution.");
        return prompt.toString();
    }
}
