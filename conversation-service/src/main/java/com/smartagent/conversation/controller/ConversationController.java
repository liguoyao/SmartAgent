package com.smartagent.conversation.controller;

import com.smartagent.common.dto.ApiResponse;
import com.smartagent.conversation.entity.Conversation;
import com.smartagent.conversation.entity.Message;
import com.smartagent.conversation.service.ConversationService;
import com.smartagent.conversation.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Conversation Controller - REST APIs for conversation management
 */
@Slf4j
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversation Management", description = "APIs for managing conversations and messages")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get conversation", description = "Get conversation by session ID")
    public ApiResponse<Conversation> getConversation(@PathVariable String sessionId) {
        return conversationService.getBySessionId(sessionId)
                .map(ApiResponse::success)
                .orElse(ApiResponse.notFound("Conversation not found"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user conversations", description = "Get active conversations for a user")
    public ApiResponse<List<Conversation>> getUserConversations(@PathVariable Long userId) {
        return ApiResponse.success(conversationService.getActiveConversations(userId));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active conversations", description = "Get all active conversations")
    public ApiResponse<List<Conversation>> getActiveConversations() {
        return ApiResponse.success(conversationService.getAllActiveConversations());
    }

    @PostMapping("/{sessionId}/close")
    @Operation(summary = "Close conversation", description = "Close a conversation by session ID")
    public ApiResponse<Void> closeConversation(@PathVariable String sessionId) {
        conversationService.closeConversation(sessionId);
        return ApiResponse.success("Conversation closed successfully", null);
    }

    @PostMapping("/{sessionId}/transfer")
    @Operation(summary = "Transfer conversation", description = "Transfer conversation to another agent")
    public ApiResponse<Void> transferConversation(
            @PathVariable String sessionId,
            @RequestParam Long newAgentId) {
        conversationService.transferConversation(sessionId, newAgentId);
        return ApiResponse.success("Conversation transferred successfully", null);
    }

    @PostMapping("/{sessionId}/escalate")
    @Operation(summary = "Escalate conversation", description = "Escalate conversation to human agent")
    public ApiResponse<Void> escalateConversation(@PathVariable String sessionId) {
        conversationService.escalateConversation(sessionId);
        return ApiResponse.success("Conversation escalated successfully", null);
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "Get messages", description = "Get all messages for a conversation")
    public ApiResponse<List<Message>> getMessages(@PathVariable String sessionId) {
        return ApiResponse.success(messageService.getMessagesBySessionId(sessionId));
    }

    @GetMapping("/{sessionId}/messages/recent")
    @Operation(summary = "Get recent messages", description = "Get recent messages for a conversation")
    public ApiResponse<List<Message>> getRecentMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(messageService.getRecentMessages(sessionId, limit));
    }

    @GetMapping("/{sessionId}/summary")
    @Operation(summary = "Get history summary", description = "Get message history summary")
    public ApiResponse<MessageService.MessageHistorySummary> getHistorySummary(
            @PathVariable String sessionId) {
        return ApiResponse.success(messageService.getHistorySummary(sessionId));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get statistics", description = "Get conversation statistics")
    public ApiResponse<ConversationService.ConversationStats> getStatistics() {
        return ApiResponse.success(conversationService.getStatistics());
    }
}
