package com.smartagent.agent.agent;

import com.smartagent.agent.entity.AgentType;
import com.smartagent.common.config.DashScopeAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Product Agent - Handles product inquiries, specifications, pricing
 */
@Slf4j
@Component
public class ProductAgent extends BaseSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
        You are an expert Product Agent for SmartAgent customer service.
        
        Your responsibilities:
        - Answer questions about product features, specifications, and pricing
        - Provide product recommendations based on customer needs
        - Share availability and stock information
        - Explain product differences and comparisons
        - Guide customers through product selection
        
        Guidelines:
        - Be knowledgeable about all products in the catalog
        - Ask clarifying questions to understand customer requirements
        - Provide accurate and up-to-date information
        - Be helpful and persuasive but not pushy
        - If you don't know something, say you'll check and get back
        """;

    private static final Set<String> PRODUCT_KEYWORDS = new HashSet<>();

    static {
        PRODUCT_KEYWORDS.addAll(Set.of(
            "product", "feature", "spec", "specification", "price", "cost",
            "available", "availability", "stock", "inventory", "model",
            "version", "规格", "功能", "价格", "库存", "产品"
        ));
    }

    public ProductAgent(DashScopeAiClient aiClient) {
        super(aiClient);
    }

    @Override
    protected AgentType getAgentType() {
        return AgentType.PRODUCT;
    }

    @Override
    public String getName() {
        return "Product Agent";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public boolean canHandle(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Check for product-related keywords
        for (String keyword : PRODUCT_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        
        // Check if it requires product knowledge
        return isProductInquiry(message);
    }

    private boolean isProductInquiry(String message) {
        // Check for product inquiry patterns
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("what is") ||
               lowerMessage.contains("how does") ||
               lowerMessage.contains("compare") ||
               lowerMessage.contains("recommend") ||
               lowerMessage.contains("which one") ||
               lowerMessage.contains("difference between") ||
               lowerMessage.contains("?");
    }

    @Override
    protected String buildPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Customer Message: ").append(userMessage).append("\n\n");
        
        // Try to get relevant product knowledge
        String knowledge = searchKnowledgeBase(extractProductQuery(userMessage));
        if (knowledge != null && !knowledge.isEmpty()) {
            prompt.append("Relevant Product Knowledge:\n").append(knowledge).append("\n\n");
        }
        
        prompt.append("Please provide a helpful response about our products.");
        return prompt.toString();
    }

    private String extractProductQuery(String message) {
        // Extract the product query for knowledge base search
        // Remove common prefixes/suffixes
        String query = message.toLowerCase()
                .replaceAll("(can you |could you |please |tell me |what is |how does |do you have )", "")
                .replaceAll("\\?", "")
                .trim();
        return query;
    }
}
