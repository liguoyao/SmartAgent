package com.smartagent.agent.controller;

import com.smartagent.agent.agent.SupervisorAgent;
import com.smartagent.agent.entity.Agent;
import com.smartagent.agent.entity.AgentType;
import com.smartagent.agent.service.AgentService;
import com.smartagent.common.dto.ApiResponse;
import com.smartagent.common.dto.PageRequest;
import com.smartagent.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent Controller - REST APIs for agent management and message processing
 */
@Slf4j
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
@Tag(name = "Agent Management", description = "APIs for managing agents and processing messages")
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/message")
    @Operation(summary = "Process message", description = "Process a customer message through the agent system")
    public ApiResponse<SupervisorAgent.AgentResponse> processMessage(
            @RequestBody Map<String, Object> request) {
        
        String message = (String) request.get("message");
        String sessionId = (String) request.get("sessionId");
        Long userId = request.get("userId") != null ? 
                Long.valueOf(request.get("userId").toString()) : null;

        if (message == null || message.trim().isEmpty()) {
            return ApiResponse.badRequest("Message is required");
        }

        if (sessionId == null) {
            sessionId = java.util.UUID.randomUUID().toString();
        }

        log.info("Processing message for session: {}", sessionId);
        SupervisorAgent.AgentResponse response = agentService.processMessage(message, sessionId, userId);
        
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "Get all agents", description = "Get all active agents")
    public ApiResponse<List<Agent>> getAllAgents() {
        return ApiResponse.success(agentService.getAllActiveAgents());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agent by ID", description = "Get agent details by ID")
    public ApiResponse<Agent> getAgentById(@PathVariable Long id) {
        return agentService.getAgentById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.notFound("Agent not found"));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get agents by type", description = "Get all agents of a specific type")
    public ApiResponse<List<Agent>> getAgentsByType(@PathVariable String type) {
        try {
            AgentType agentType = AgentType.valueOf(type.toUpperCase());
            return ApiResponse.success(agentService.getAgentsByType(agentType));
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest("Invalid agent type: " + type);
        }
    }

    @PostMapping
    @Operation(summary = "Create agent", description = "Create a new agent")
    public ApiResponse<Agent> createAgent(@RequestBody Agent agent) {
        try {
            Agent created = agentService.createAgent(agent);
            return ApiResponse.success("Agent created successfully", created);
        } catch (Exception e) {
            log.error("Error creating agent", e);
            return ApiResponse.serverError("Failed to create agent: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update agent", description = "Update an existing agent")
    public ApiResponse<Agent> updateAgent(@PathVariable Long id, @RequestBody Agent agent) {
        try {
            agent.setId(id);
            Agent updated = agentService.updateAgent(agent);
            return ApiResponse.success("Agent updated successfully", updated);
        } catch (Exception e) {
            log.error("Error updating agent", e);
            return ApiResponse.serverError("Failed to update agent: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate agent", description = "Deactivate an agent")
    public ApiResponse<Void> deactivateAgent(@PathVariable Long id) {
        try {
            agentService.deactivateAgent(id);
            return ApiResponse.success("Agent deactivated successfully", null);
        } catch (Exception e) {
            log.error("Error deactivating agent", e);
            return ApiResponse.serverError("Failed to deactivate agent: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get agent statistics", description = "Get statistics for all agent types")
    public ApiResponse<Map<String, Object>> getAgentStatistics() {
        return ApiResponse.success(agentService.getAgentStatistics());
    }

    @PostMapping("/classify")
    @Operation(summary = "Classify intent", description = "Classify the intent of a message (for testing)")
    public ApiResponse<String> classifyIntent(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ApiResponse.badRequest("Message is required");
        }
        
        String intent = agentService.classifyIntent(message);
        return ApiResponse.success(intent);
    }
}
