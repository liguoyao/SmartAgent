package com.smartagent.conversation.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagent.conversation.entity.Conversation;
import com.smartagent.conversation.entity.Message;
import com.smartagent.conversation.service.ConversationService;
import com.smartagent.conversation.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time chat
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    // Map of sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // Map of userId -> sessionId
    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, sessionId);
            log.info("WebSocket connection established: sessionId={}, userId={}", sessionId, userId);
        } else {
            log.info("WebSocket connection established: sessionId={} (anonymous)", sessionId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String payload = textMessage.getPayload();
        log.debug("Received message from session {}: {}", session.getId(), payload);

        try {
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
            
            switch (chatMessage.getType()) {
                case "MESSAGE":
                    handleChatMessage(session, chatMessage);
                    break;
                case "TYPING":
                    handleTypingIndicator(session, chatMessage);
                    break;
                case "READ":
                    handleReadReceipt(session, chatMessage);
                    break;
                case "PING":
                    handlePing(session);
                    break;
                default:
                    log.warn("Unknown message type: {}", chatMessage.getType());
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            sendError(session, "Failed to process message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
        }
        
        log.info("WebSocket connection closed: sessionId={}, status={}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
    }

    private void handleChatMessage(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        Long userId = getUserIdFromSession(session);
        String sessionId = chatMessage.getSessionId() != null ? 
                chatMessage.getSessionId() : session.getId();

        // Create or get conversation
        Conversation conversation = conversationService.getOrCreateConversation(
                sessionId, userId, chatMessage.getAgentId());

        // Save user message
        Message userMessage = messageService.saveMessage(
                conversation.getId(),
                sessionId,
                Message.SenderType.USER,
                userId,
                chatMessage.getContent(),
                Message.MessageType.TEXT,
                null, null, null, null
        );

        // Send to agent service for processing via Feign
        // This would be async in production
        sendToAgentService(conversation, userMessage);

        // Send acknowledgment to user
        ChatMessage ack = new ChatMessage();
        ack.setType("ACK");
        ack.setMessageId(userMessage.getId());
        ack.setSessionId(sessionId);
        sendMessage(session, ack);
    }

    private void handleTypingIndicator(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        // Broadcast typing indicator to other participants
        String sessionId = chatMessage.getSessionId();
        if (sessionId != null) {
            ChatMessage typingMsg = new ChatMessage();
            typingMsg.setType("TYPING");
            typingMsg.setSessionId(sessionId);
            typingMsg.setSenderId(getUserIdFromSession(session));
            typingMsg.setIsTyping(chatMessage.getIsTyping());
            
            // Broadcast to all sessions in this conversation
            broadcastToConversation(sessionId, typingMsg);
        }
    }

    private void handleReadReceipt(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        Long userId = getUserIdFromSession(session);
        if (chatMessage.getMessageId() != null) {
            messageService.markAsRead(chatMessage.getMessageId());
            
            ChatMessage readAck = new ChatMessage();
            readAck.setType("READ_ACK");
            readAck.setMessageId(chatMessage.getMessageId());
            readAck.setReadAt(java.time.LocalDateTime.now());
            
            sendMessage(session, readAck);
        }
    }

    private void handlePing(WebSocketSession session) throws IOException {
        ChatMessage pong = new ChatMessage();
        pong.setType("PONG");
        pong.setTimestamp(System.currentTimeMillis());
        sendMessage(session, pong);
    }

    private void sendToAgentService(Conversation conversation, Message message) {
        // This would call the agent-service via Feign
        // For now, it's handled asynchronously
        log.debug("Sending message to agent service for processing");
    }

    public void sendAgentResponse(String sessionId, String response, String agentType) {
        WebSocketSession session = getSessionBySessionId(sessionId);
        if (session != null && session.isOpen()) {
            try {
                ChatMessage agentMsg = new ChatMessage();
                agentMsg.setType("MESSAGE");
                agentMsg.setSessionId(sessionId);
                agentMsg.setContent(response);
                agentMsg.setSenderType("AGENT");
                agentMsg.setAgentType(agentType);
                agentMsg.setTimestamp(System.currentTimeMillis());
                
                sendMessage(session, agentMsg);
            } catch (IOException e) {
                log.error("Error sending agent response", e);
            }
        }
    }

    private void broadcastToConversation(String conversationSessionId, ChatMessage message) throws IOException {
        WebSocketSession session = getSessionBySessionId(conversationSessionId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        }
    }

    private void sendMessage(WebSocketSession session, ChatMessage message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }

    private void sendError(WebSocketSession session, String error) throws IOException {
        ChatMessage errorMsg = new ChatMessage();
        errorMsg.setType("ERROR");
        errorMsg.setError(error);
        sendMessage(session, errorMsg);
    }

    private WebSocketSession getSessionBySessionId(String sessionId) {
        // Find session by matching conversation sessionId
        for (WebSocketSession s : sessions.values()) {
            if (s.getId().equals(sessionId)) {
                return s;
            }
        }
        return null;
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Long) {
            return (Long) userId;
        } else if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return null;
    }

    /**
     * Chat message DTO for WebSocket communication
     */
    @lombok.Data
    public static class ChatMessage {
        private String type;
        private Long messageId;
        private String sessionId;
        private Long senderId;
        private String senderType;
        private String content;
        private String agentType;
        private Boolean isTyping;
        private Long timestamp;
        private String error;
        private java.time.LocalDateTime readAt;
    }
}
