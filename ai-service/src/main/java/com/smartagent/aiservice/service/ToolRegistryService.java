package com.smartagent.aiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Service
public class ToolRegistryService {

    private final Map<String, ToolFunction> tools = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    public void init() {
        register("search_knowledge", this::searchKnowledge);
        register("create_ticket", this::createTicket);
        register("get_order_status", this::getOrderStatus);
        register("transfer_to_human", this::transferToHuman);
        log.info("Tool registry initialized with {} tools", tools.size());
    }

    public void register(String name, ToolFunction function) {
        tools.put(name, function);
        log.debug("Tool registered: {}", name);
    }

    public Map<String, Object> execute(String toolName, Map<String, Object> args) {
        ToolFunction tool = tools.get(toolName);
        if (tool == null) {
            return Map.of("error", "Tool '" + toolName + "' not found");
        }
        
        try {
            Object result = tool.apply(args);
            log.info("Tool executed successfully: {}", toolName);
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            return Map.of("result", result);
        } catch (Exception e) {
            log.error("Tool execution failed: {}", toolName, e);
            return Map.of("error", e.getMessage());
        }
    }

    public Map<String, Object> execute(String toolName, String... requiredKeys, Object... values) {
        Map<String, Object> args = new HashMap<>();
        for (int i = 0; i < requiredKeys.length; i++) {
            args.put(requiredKeys[i], values[i]);
        }
        return execute(toolName, args);
    }

    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }

    public Map<String, String> getToolDescriptions() {
        return Map.of(
            "search_knowledge", "Search knowledge base for relevant documents and information",
            "create_ticket", "Create a support ticket in the ticketing system",
            "get_order_status", "Get the current status of an order",
            "transfer_to_human", "Transfer the conversation to a human agent"
        );
    }

    // Tool implementations

    private Map<String, Object> searchKnowledge(Map<String, Object> args) {
        String query = (String) args.get("query");
        // This will be called via RAG service
        return Map.of(
            "query", query,
            "results", List.of(),
            "message", "search_knowledge tool called - implementation delegated to agent"
        );
    }

    private Map<String, Object> createTicket(Map<String, Object> args) {
        String category = (String) args.get("category");
        String priority = (String) args.get("priority");
        String summary = (String) args.get("summary");
        
        String ticketId = "TICKET-" + (Math.abs(summary.hashCode()) % 100000);
        
        log.info("Ticket created: {} [{}] {}", ticketId, priority, summary);
        
        return Map.of(
            "ticketId", ticketId,
            "category", category != null ? category : "general",
            "priority", priority != null ? priority : "medium",
            "summary", summary,
            "status", "open",
            "message", "Ticket " + ticketId + " created successfully. Our team will respond within 24 hours."
        );
    }

    private Map<String, Object> getOrderStatus(Map<String, Object> args) {
        String orderId = (String) args.get("orderId");
        
        // Simulated response
        List<String> statuses = List.of("processing", "shipped", "delivered", "cancelled");
        String status = statuses.get(Math.abs(orderId.hashCode()) % statuses.size());
        
        log.info("Order status checked: {} -> {}", orderId, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("status", status);
        result.put("estimatedDelivery", status.equals("delivered") ? "2024-01-10" : "2024-01-15");
        result.put("message", "Your order " + orderId + " is currently " + status + ".");
        
        if ("shipped".equals(status)) {
            result.put("trackingNumber", "TRK-" + orderId + "-12345");
        }
        
        return result;
    }

    private Map<String, Object> transferToHuman(Map<String, Object> args) {
        String context = (String) args.get("context");
        
        log.info("Transfer to human requested");
        
        return Map.of(
            "transferId", "TRANSFER-" + (Math.abs(context.hashCode()) % 100000),
            "status", "pending",
            "queuePosition", (Math.abs(context.hashCode()) % 5) + 1,
            "estimatedWait", "2-3 minutes",
            "message", "I'm connecting you with a human agent. Please hold on."
        );
    }

    @FunctionalInterface
    public interface ToolFunction extends Function<Map<String, Object>, Object> {}
}