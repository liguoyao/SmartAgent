package com.smartagent.conversation.service;

import com.smartagent.conversation.entity.Message;
import com.smartagent.conversation.entity.Message.ContentType;
import com.smartagent.conversation.entity.Message.MessageType;
import com.smartagent.conversation.entity.Message.SenderType;
import com.smartagent.conversation.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Message service for handling chat messages
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    /**
     * Save a new message
     */
    @Transactional
    public Message saveMessage(Long conversationId, String sessionId, SenderType senderType,
                               Long senderId, String content, MessageType messageType,
                               String agentType, String intent, Double confidence,
                               Long processingTimeMs) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .sessionId(sessionId)
                .senderType(senderType)
                .senderId(senderId)
                .content(content)
                .messageType(messageType)
                .contentType(ContentType.TEXT)
                .agentType(agentType)
                .intent(intent)
                .confidence(confidence)
                .processingTimeMs(processingTimeMs)
                .build();
        
        messageRepository.insert(message);
        log.debug("Saved message: id={}, sessionId={}, type={}", 
                message.getId(), sessionId, senderType);
        
        return message;
    }

    /**
     * Save an agent response message
     */
    public Message saveAgentMessage(Long conversationId, String sessionId, String content,
                                    String agentType, String intent, Double confidence,
                                    Long processingTimeMs) {
        return saveMessage(conversationId, sessionId, SenderType.AGENT, null, content,
                MessageType.TEXT, agentType, intent, confidence, processingTimeMs);
    }

    /**
     * Save a system message
     */
    public Message saveSystemMessage(Long conversationId, String sessionId, String content) {
        return saveMessage(conversationId, sessionId, SenderType.SYSTEM, null, content,
                MessageType.SYSTEM, null, null, null, null);
    }

    /**
     * Get messages by session ID
     */
    public List<Message> getMessagesBySessionId(String sessionId) {
        return messageRepository.findBySessionId(sessionId);
    }

    /**
     * Get messages by conversation ID
     */
    public List<Message> getMessagesByConversationId(Long conversationId) {
        return messageRepository.findByConversationId(conversationId);
    }

    /**
     * Get recent messages for a session
     */
    public List<Message> getRecentMessages(String sessionId, int limit) {
        return messageRepository.findRecentBySessionId(sessionId, limit);
    }

    /**
     * Get message by ID
     */
    public Optional<Message> getMessageById(Long id) {
        return Optional.ofNullable(messageRepository.selectById(id));
    }

    /**
     * Mark message as read
     */
    @Transactional
    public void markAsRead(Long messageId) {
        Optional.ofNullable(messageRepository.selectById(messageId)).ifPresent(message -> {
            // Mark as read logic if needed
            log.debug("Marked message as read: id={}", messageId);
        });
    }

    /**
     * Count messages in session
     */
    public Long countMessages(String sessionId) {
        return messageRepository.countBySessionId(sessionId);
    }

    /**
     * Get message history summary
     */
    public MessageHistorySummary getHistorySummary(String sessionId) {
        List<Message> messages = messageRepository.findBySessionId(sessionId);
        
        long userMessages = messages.stream()
                .filter(m -> m.getSenderType() == SenderType.USER)
                .count();
        
        long agentMessages = messages.stream()
                .filter(m -> m.getSenderType() == SenderType.AGENT)
                .count();
        
        long systemMessages = messages.stream()
                .filter(m -> m.getSenderType() == SenderType.SYSTEM)
                .count();
        
        return new MessageHistorySummary(sessionId, messages.size(), 
                userMessages, agentMessages, systemMessages);
    }

    /**
     * Message history summary record
     */
    public record MessageHistorySummary(
            String sessionId,
            long totalMessages,
            long userMessages,
            long agentMessages,
            long systemMessages
    ) {}
}
