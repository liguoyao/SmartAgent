package com.smartagent.aiservice.service;

import com.smartagent.aiservice.model.AgentResponse;
import com.smartagent.aiservice.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SpecialistAgentService {

    private final LLMClientService llmClient;
    private final ConversationMemoryService memory;
    
    private static final Map<String, String> SPECIALTIES = Map.of(
        "billing", "billing and payment inquiries, refunds, invoices",
        "technical", "technical support, troubleshooting, account issues",
        "orders", "order management, shipping, returns",
        "general", "general inquiries and information requests"
    );

    private final Map<String, String> systemPrompts = new HashMap<>();

    public SpecialistAgentService(LLMClientService llmClient, ConversationMemoryService memory) {
        this.llmClient = llmClient;
        this.memory = memory;
        
        // Initialize system prompts for each specialty
        for (Map.Entry<String, String> entry : SPECIALTIES.entrySet()) {
            String specialty = entry.getKey();
            String description = entry.getValue();
            systemPrompts.put(specialty, String.format("""
                You are a specialized AI assistant focusing on %s. You have deep knowledge in this area and handle related requests.

                ## Your Specialty: %s

                ## Responsibilities
                - Handle %s-related inquiries
                - Provide accurate, detailed information
                - Create appropriate tickets when needed
                - Know when to escalate to supervisors or humans

                ## Guidelines
                - Be thorough but concise
                - Use technical terminology appropriately
                - Provide step-by-step guidance when applicable
                - Always confirm understanding before concluding
                """, description, specialty, specialty));
        }
    }

    public AgentResponse handle(String userMessage, String sessionId, String specialty) {
        log.info("Specialist handling: specialty={}, session={}", specialty, sessionId);
        
        String effectiveSpecialty = specialty != null && SPECIALTIES.containsKey(specialty) 
            ? specialty : "general";
        
        String systemPrompt = systemPrompts.getOrDefault(effectiveSpecialty, systemPrompts.get("general"));
        List<Map<String, Object>> history = memory.getContextForLLM(sessionId, 10);
        
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        
        try {
            Map<String, Object> response = llmClient.chat(messages, getToolDefinitions());
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return AgentResponse.builder()
                    .message("I'm having trouble processing your request. Please try again.")
                    .metadata(Map.of("specialty", effectiveSpecialty))
                    .build();
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = message != null ? (String) message.get("content") : "";
            
            return AgentResponse.builder()
                .message(content)
                .metadata(Map.of("specialty", effectiveSpecialty))
                .build();
                
        } catch (Exception e) {
            log.error("Specialist agent error", e);
            return AgentResponse.builder()
                .message("I apologize, but I'm having trouble processing your request right now.")
                .metadata(Map.of("error", e.getMessage(), "specialty", effectiveSpecialty))
                .build();
        }
    }

    public Set<String> getAvailableSpecialties() {
        return SPECIALTIES.keySet();
    }

    public String getSpecialtyDescription(String specialty) {
        return SPECIALTIES.getOrDefault(specialty, "General inquiry handling");
    }

    public Map<String, String> getAllSpecialties() {
        return SPECIALTIES;
    }

    private List<Map<String, Object>> getToolDefinitions() {
        return List.of(
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "search_knowledge",
                    "description", "Search the knowledge base for relevant documents",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "query", Map.of("type", "string", "description", "The search query")
                        ),
                        "required", List.of("query")
                    )
                )
            ),
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "create_ticket",
                    "description", "Create a support ticket",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "category", Map.of("type", "string", "description", "Ticket category"),
                            "priority", Map.of("type", "string", "description", "Priority level"),
                            "summary", Map.of("type", "string", "description", "Brief summary")
                        ),
                        "required", List.of("category", "priority", "summary")
                    )
                )
            )
        );
    }
}