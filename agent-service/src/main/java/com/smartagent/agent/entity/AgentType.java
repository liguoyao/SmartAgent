package com.smartagent.agent.entity;

/**
 * Agent types enum
 */
public enum AgentType {
    SUPERVISOR("Supervisor Agent", "Main orchestrator that routes customer requests"),
    PRODUCT("Product Agent", "Handles product inquiries, specifications, pricing"),
    COMPLAINT("Complaint Agent", "Handles customer complaints and feedback"),
    REFUND("Refund Agent", "Processes refund requests and tracking"),
    ORDER("Order Agent", "Manages order status, modifications, cancellations"),
    TECHNICAL("Technical Agent", "Handles technical support and troubleshooting"),
    BILLING("Billing Agent", "Manages billing inquiries and payment issues"),
    ESCALATION("Escalation Agent", "Handles complex issues requiring human intervention");

    private final String displayName;
    private final String description;

    AgentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
