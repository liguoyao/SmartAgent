package com.smartagent.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagent.aiservice.model.AgentResponse;
import com.smartagent.aiservice.model.Intent;
import com.smartagent.aiservice.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SupervisorAgentService {

    private final LLMClientService llmClient;
    private final ConversationMemoryService memory;
    private final ToolRegistryService toolRegistry;
    private final RAGService ragService;
    
    private final String systemPrompt = """
You are SmartAgent, an intelligent customer service assistant. Your role is to efficiently understand user requests and coordinate appropriate responses.

## Your Capabilities
- Answer questions using the knowledge base (search_knowledge)
- Create support tickets (create_ticket)
- Check order status (get_order_status)
- Transfer to human agents when needed (transfer_to_human)

## Guidelines
1. Be professional, helpful, and concise
2. Always try to help using available tools before transferring
3. When using search_knowledge, formulate a clear, specific query
4. For ticket creation, summarize the issue clearly and assign appropriate priority
5. Escalate to humans for sensitive matters, complex troubleshooting, or explicit requests

## Response Format
- For simple questions: Direct answer
- For tool use: Indicate what you're doing, use the tool, then respond with results
- For transfers: Explain why and confirm the transfer

Always maintain context of the conversation history provided.""";

    public SupervisorAgentService(
            LLMClientService llmClient,
            ConversationMemoryService memory,
            ToolRegistryService toolRegistry,
            RAGService ragService) {
        this.llmClient = llmClient;
        this.memory = memory;
        this.toolRegistry = toolRegistry;
        this.ragService = ragService;
    }

    public Intent classifyIntent(String conversation) {
        try {
            String prompt = "Classify the user's intent from this conversation:\n\n" +
                "Conversation:\n" + conversation + "\n\n" +
                "Available intents:\n" +
                "- QUERY: User is asking a question or seeking information\n" +
                "- TICKET_CREATE: User wants to create a support ticket\n" +
                "- ORDER_STATUS: User is asking about an order\n" +
                "- TRANSFER: User is requesting human assistance\n" +
                "- GREETING: User is greeting or casual conversation\n" +
                "- FEEDBACK: User is providing feedback\n" +
                "- OTHER: None of the above\n\n" +
                "Respond with only the intent label (e.g., QUERY, TICKET_CREATE).";
            
            String response = llmClient.chatSimple(prompt, 
                "You are an intent classifier. Respond with only the intent label.");
            
            String intentStr = response.trim().toUpperCase();
            
            if (intentStr.contains("QUERY")) return Intent.QUERY;
            if (intentStr.contains("TICKET_CREATE")) return Intent.TICKET_CREATE;
            if (intentStr.contains("ORDER_STATUS")) return Intent.ORDER_STATUS;
            if (intentStr.contains("TRANSFER")) return Intent.TRANSFER;
            if (intentStr.contains("GREETING")) return Intent.GREETING;
            if (intentStr.contains("FEEDBACK")) return Intent.FEEDBACK;
            
            return Intent.OTHER;
        } catch (Exception e) {
            log.error("Intent classification failed", e);
            return Intent.OTHER;
        }
    }

    public AgentResponse processMessage(String userMessage, String sessionId, boolean useTools) {
        log.info("Processing message for session: {}, useTools: {}", sessionId, useTools);
        
        List<Map<String, Object>> history = memory.getContextForLLM(sessionId, 10);
        
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        
        if (!useTools) {
            String response = llmClient.chatSimple(userMessage, systemPrompt + "\n\nHistory:\n" + formatHistory(history));
            return AgentResponse.builder()
                .message(response)
                .build();
        }
        
        try {
            Map<String, Object> response = llmClient.chat(messages, getToolDefinitions());
            String content = extractContent(response);
            List<Map<String, Object>> toolCalls = extractToolCalls(response);
            
            if (toolCalls != null && !toolCalls.isEmpty()) {
                return executeTools(toolCalls, content, sessionId, messages);
            }
            
            return AgentResponse.builder()
                .message(content)
                .build();
                
        } catch (Exception e) {
            log.error("LLM error in supervisor", e);
            return AgentResponse.builder()
                .message("I apologize, but I'm having trouble processing your request right now. Please try again.")
                .metadata(Map.of("error", e.getMessage()))
                .build();
        }
    }

    private AgentResponse executeTools(List<Map<String, Object>> toolCalls, String content, 
            String sessionId, List<Map<String, Object>> messages) throws com.fasterxml.jackson.core.JsonProcessingException {
        
        List<Map<String, Object>> toolResults = new ArrayList<>();
        
        for (Map<String, Object> toolCall : toolCalls) {
            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
            String toolName = (String) function.get("name");
            String argsJson = (String) function.get("arguments");
            
            log.info("Executing tool: {}", toolName);
            
            Map<String, Object> args;
            try {
                args = new ObjectMapper().readValue(argsJson, Map.class);
            } catch (Exception e) {
                args = Map.of();
            }
            
            // Handle search_knowledge specially - delegate to RAG service
            if ("search_knowledge".equals(toolName)) {
                String query = (String) args.get("query");
                var docs = ragService.search(query, null);
                Map<String, Object> result = new HashMap<>();
                result.put("query", query);
                result.put("count", docs.size());
                result.put("results", docs.stream()
                    .map(d -> Map.of(
                        "content", d.getContent(),
                        "source", d.getMetadata() != null ? d.getMetadata().getOrDefault("source", "unknown") : "unknown",
                        "distance", d.getMetadata() != null ? d.getMetadata().getOrDefault("distance", 0) : 0
                    ))
                    .collect(Collectors.toList()));
                toolResults.add(Map.of("call", toolName, "result", result));
            } else {
                Map<String, Object> result = toolRegistry.execute(toolName, args);
                toolResults.add(Map.of("call", toolName, "result", result));
            }
        }
        
        // Add assistant message to memory
        memory.addMessage(sessionId, Message.builder()
            .role("assistant")
            .content(content)
            .toolCalls(toolCalls)
            .build());
        
        // Add tool results to memory
        for (Map<String, Object> tr : toolResults) {
            memory.addMessage(sessionId, Message.builder()
                .role("tool")
                .content(new ObjectMapper().writeValueAsString(tr.get("result")))
                .name((String) tr.get("call"))
                .toolCallId("call_" + tr.get("call").hashCode())
                .build());
        }
        
        // Generate final response with tool results
        List<Map<String, Object>> finalMessages = new ArrayList<>(messages);
        finalMessages.add(Map.of("role", "assistant", "content", content));
        finalMessages.add(Map.of("role", "system", "content", 
            "Tool results: " + new ObjectMapper().writeValueAsString(toolResults)));
        
        String finalResponse;
        try {
            finalResponse = llmClient.chatSimple(
                "Based on the tool results, provide a helpful response to the user. " +
                "Tool results: " + new ObjectMapper().writeValueAsString(toolResults),
                systemPrompt);
        } catch (Exception e) {
            finalResponse = "I've processed your request. Is there anything else I can help you with?";
        }
        
        boolean shouldTransfer = toolResults.stream()
            .anyMatch(tr -> "transfer_to_human".equals(tr.get("call")));
        
        Map<String, Object> firstResult = toolResults.isEmpty() ? null : 
            (Map<String, Object>) toolResults.get(0).get("result");
        
        return AgentResponse.builder()
            .message(finalResponse)
            .toolUsed((String) (toolResults.isEmpty() ? null : toolResults.get(0).get("call")))
            .toolResult(firstResult)
            .shouldTransfer(shouldTransfer)
            .metadata(Map.of("toolResults", toolResults))
            .build();
    }

    private String formatHistory(List<Map<String, Object>> history) {
        if (history.isEmpty()) return "No previous messages.";
        return history.stream()
            .map(m -> m.get("role") + ": " + m.get("content"))
            .collect(Collectors.joining("\n"));
    }

    public List<Map<String, Object>> getToolDefinitions() {
        return List.of(
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "search_knowledge",
                    "description", "Search the knowledge base for relevant documents and information. Use this when you need to find specific information, policies, or guidelines to answer user questions.",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "query", Map.of(
                                "type", "string",
                                "description", "The search query to find relevant knowledge base documents."
                            )
                        ),
                        "required", List.of("query")
                    )
                )
            ),
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "create_ticket",
                    "description", "Create a support ticket in the ticketing system. Use this when the user requests to create a ticket, reports an issue that needs tracking, or needs formal documentation of a request.",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "category", Map.of(
                                "type", "string",
                                "enum", List.of("technical", "billing", "general", "feedback", "complaint"),
                                "description", "Category of the ticket."
                            ),
                            "priority", Map.of(
                                "type", "string",
                                "enum", List.of("low", "medium", "high", "urgent"),
                                "description", "Priority level of the ticket."
                            ),
                            "summary", Map.of(
                                "type", "string",
                                "description", "Brief summary of the issue or request (max 200 characters)."
                            )
                        ),
                        "required", List.of("category", "priority", "summary")
                    )
                )
            ),
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "get_order_status",
                    "description", "Get the current status of an order. Use this when the user asks about order delivery, shipping status, or order details.",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "orderId", Map.of(
                                "type", "string",
                                "description", "The unique order identifier."
                            )
                        ),
                        "required", List.of("orderId")
                    )
                )
            ),
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "transfer_to_human",
                    "description", "Transfer the conversation to a human agent. Use this when the request is complex, sensitive, requires human judgment, or the user explicitly requests human assistance.",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "context", Map.of(
                                "type", "string",
                                "description", "Summary of the conversation context to provide to the human agent."
                            )
                        ),
                        "required", List.of("context")
                    )
                )
            )
        );
    }

    private String extractContent(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "";
        
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message != null ? (String) message.get("content") : "";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractToolCalls(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return Collections.emptyList();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) return Collections.emptyList();
        
        return (List<Map<String, Object>>) message.get("tool_calls");
    }
}
