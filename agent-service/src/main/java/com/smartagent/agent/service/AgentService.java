package com.smartagent.agent.service;

import com.smartagent.agent.agent.SpecialistAgent;
import com.smartagent.agent.agent.SupervisorAgent;
import com.smartagent.agent.entity.Agent;
import com.smartagent.agent.entity.AgentConversationContext;
import com.smartagent.agent.entity.AgentType;
import com.smartagent.agent.repository.AgentRepository;
import com.smartagent.common.config.DashScopeAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent Service - Core service for agent management and message processing
 */
@Slf4j
@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final SupervisorAgent supervisorAgent;
    private final Map<AgentType, SpecialistAgent> specialistAgents;
    private final DashScopeAiClient aiClient;

    public AgentService(AgentRepository agentRepository, 
                        SupervisorAgent supervisorAgent,
                        List<SpecialistAgent> specialists,
                        DashScopeAiClient aiClient) {
        this.agentRepository = agentRepository;
        this.supervisorAgent = supervisorAgent;
        this.aiClient = aiClient;
        
        // Build specialist agents map
        this.specialistAgents = new ConcurrentHashMap<>();
        for (SpecialistAgent specialist : specialists) {
            this.specialistAgents.put(specialist.getType(), specialist);
            log.info("Registered specialist agent: {}", specialist.getType());
        }
    }

    /**
     * Process a customer message through the agent system
     */
    public SupervisorAgent.AgentResponse processMessage(String message, String sessionId, Long userId) {
        log.info("Processing message for session: {}, user: {}", sessionId, userId);
        
        // Create conversation context
        createConversationContext(sessionId, userId);
        
        // Process through supervisor agent
        SupervisorAgent.AgentResponse response = supervisorAgent.processMessage(message, sessionId, userId);
        
        // Update conversation context
        updateConversationContext(sessionId, response);
        
        return response;
    }

    /**
     * Get all active agents
     */
    public List<Agent> getAllActiveAgents() {
        return agentRepository.findAllActiveOrderByPriority();
    }

    /**
     * Get agents by type
     */
    public List<Agent> getAgentsByType(AgentType type) {
        return agentRepository.findByType(type);
    }

    /**
     * Get agent by ID
     */
    public Optional<Agent> getAgentById(Long id) {
        return agentRepository.findByIdActive(id);
    }

    /**
     * Create a new agent
     */
    @Transactional
    public Agent createAgent(Agent agent) {
        agent.setIsActive(true);
        agent.setCurrentConversations(0);
        agent.setTotalHandled(0L);
        agent.setSuccessRate(0.0);
        agent.setAvgResponseTimeMs(0L);
        
        agentRepository.insert(agent);
        log.info("Created new agent: {} of type {}", agent.getName(), agent.getType());
        
        return agent;
    }

    /**
     * Update agent
     */
    @Transactional
    public Agent updateAgent(Agent agent) {
        agentRepository.updateById(agent);
        log.info("Updated agent: {}", agent.getId());
        return agent;
    }

    /**
     * Deactivate agent
     */
    @Transactional
    public void deactivateAgent(Long agentId) {
        Agent agent = agentRepository.selectById(agentId);
        if (agent != null) {
            agent.setIsActive(false);
            agentRepository.updateById(agent);
            log.info("Deactivated agent: {}", agentId);
        }
    }

    /**
     * Get agent statistics
     */
    public Map<String, Object> getAgentStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        for (AgentType type : AgentType.values()) {
            List<Agent> agents = agentRepository.findByType(type);
            int activeCount = (int) agents.stream().filter(Agent::getIsActive).count();
            stats.put(type.name(), Map.of(
                "total", agents.size(),
                "active", activeCount
            ));
        }
        
        return stats;
    }

    /**
     * Classify intent without processing (for preview/testing)
     */
    public String classifyIntent(String message) {
        String[] categories = {"PRODUCT", "COMPLAINT", "REFUND", "ORDER", "TECHNICAL", "BILLING", "ESCALATION"};
        return aiClient.classifyIntent(message, categories);
    }

    private void createConversationContext(String sessionId, Long userId) {
        // In production, save to database
        log.debug("Creating conversation context for session: {}", sessionId);
    }

    private void updateConversationContext(String sessionId, SupervisorAgent.AgentResponse response) {
        // In production, update database
        log.debug("Updated conversation context for session: {} with response type: {}", 
                sessionId, response.getRoutedTo());
    }
}
