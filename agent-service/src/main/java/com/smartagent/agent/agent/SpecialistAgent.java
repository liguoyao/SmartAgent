package com.smartagent.agent.agent;

import com.smartagent.agent.entity.AgentType;

/**
 * Interface for Specialist Agents
 */
public interface SpecialistAgent {

    /**
     * Get the agent type this specialist handles
     */
    AgentType getType();

    /**
     * Check if this agent can handle the given message
     */
    boolean canHandle(String message);

    /**
     * Process the message and return a response
     */
    SupervisorAgent.AgentResponse process(String message, String sessionId, Long userId);

    /**
     * Get the system prompt for this agent
     */
    String getSystemPrompt();

    /**
     * Get the agent name
     */
    String getName();
}
