package com.smartagent.aiservice.service;

import com.smartagent.aiservice.model.AgentResponse;
import com.smartagent.aiservice.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    private final SupervisorAgentService supervisorAgent;
    private final SpecialistAgentService specialistAgent;
    private final ConversationMemoryService memory;

    public ChatService(
            SupervisorAgentService supervisorAgent,
            SpecialistAgentService specialistAgent,
            ConversationMemoryService memory) {
        this.supervisorAgent = supervisorAgent;
        this.specialistAgent = specialistAgent;
        this.memory = memory;
    }

    public ChatResult processChat(String message, String sessionId, boolean useTools, String specialty) {
        log.info("Processing chat: sessionId={}, useTools={}, specialty={}", sessionId, useTools, specialty);
        
        // Generate session ID if not provided
        String effectiveSessionId = sessionId != null && !sessionId.isBlank() ? sessionId : UUID.randomUUID().toString();
        
        // Add user message to memory
        memory.addMessage(effectiveSessionId, Message.builder()
            .role("user")
            .content(message)
            .build());
        
        AgentResponse response;
        
        // Route to appropriate agent
        if (specialty != null && specialistAgent.getAvailableSpecialties().contains(specialty)) {
            response = specialistAgent.handle(message, effectiveSessionId, specialty);
        } else {
            response = supervisorAgent.processMessage(message, effectiveSessionId, useTools);
        }
        
        // Add assistant response to memory
        memory.addMessage(effectiveSessionId, Message.builder()
            .role("assistant")
            .content(response.getMessage())
            .build());
        
        // Extend TTL for active session
        memory.extendTtl(effectiveSessionId);
        
        return new ChatResult(
            response.getMessage(),
            effectiveSessionId,
            response.getToolUsed(),
            response.getToolResult(),
            response.isShouldTransfer(),
            response.getIntent() != null ? response.getIntent().name() : null,
            response.getMetadata()
        );
    }

    public record ChatResult(
        String message,
        String sessionId,
        String toolUsed,
        Object toolResult,
        boolean shouldTransfer,
        String intent,
        Object metadata
    ) {}
}