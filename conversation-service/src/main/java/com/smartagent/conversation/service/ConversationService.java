package com.smartagent.conversation.service;

import com.smartagent.conversation.entity.Conversation;
import com.smartagent.conversation.entity.Conversation.ConversationStatus;
import com.smartagent.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Conversation service for managing chat sessions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    /**
     * Get or create a conversation for the given session
     */
    @Transactional
    public Conversation getOrCreateConversation(String sessionId, Long userId, Long agentId) {
        Optional<Conversation> existing = conversationRepository.findBySessionIdActive(sessionId);
        
        if (existing.isPresent()) {
            Conversation conversation = existing.get();
            // Update last message time
            conversation.setLastMessageAt(LocalDateTime.now());
            conversation.setMessageCount(conversation.getMessageCount() + 1);
            conversationRepository.updateById(conversation);
            return conversation;
        }
        
        // Create new conversation
        Conversation conversation = Conversation.builder()
                .sessionId(sessionId)
                .userId(userId)
                .agentId(agentId)
                .status(ConversationStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .messageCount(1)
                .isActive(true)
                .build();
        
        conversationRepository.insert(conversation);
        log.info("Created new conversation: sessionId={}, userId={}", sessionId, userId);
        
        return conversation;
    }

    /**
     * Get conversation by session ID
     */
    public Optional<Conversation> getBySessionId(String sessionId) {
        return conversationRepository.findBySessionIdActive(sessionId);
    }

    /**
     * Get active conversations for a user
     */
    public List<Conversation> getActiveConversations(Long userId) {
        return conversationRepository.findActiveByUserId(userId);
    }

    /**
     * Get all active conversations
     */
    public List<Conversation> getAllActiveConversations() {
        return conversationRepository.findAllActive();
    }

    /**
     * Close a conversation
     */
    @Transactional
    public void closeConversation(String sessionId) {
        conversationRepository.findBySessionIdActive(sessionId).ifPresent(conversation -> {
            conversation.setStatus(ConversationStatus.CLOSED);
            conversation.setIsActive(false);
            conversation.setEndedAt(LocalDateTime.now());
            conversationRepository.updateById(conversation);
            log.info("Closed conversation: sessionId={}", sessionId);
        });
    }

    /**
     * Transfer conversation to another agent
     */
    @Transactional
    public void transferConversation(String sessionId, Long newAgentId) {
        conversationRepository.findBySessionIdActive(sessionId).ifPresent(conversation -> {
            conversation.setStatus(ConversationStatus.TRANSFERRED);
            conversation.setAgentId(newAgentId);
            conversationRepository.updateById(conversation);
            log.info("Transferred conversation: sessionId={}, newAgentId={}", sessionId, newAgentId);
        });
    }

    /**
     * Escalate conversation
     */
    @Transactional
    public void escalateConversation(String sessionId) {
        conversationRepository.findBySessionIdActive(sessionId).ifPresent(conversation -> {
            conversation.setStatus(ConversationStatus.ESCALATED);
            conversationRepository.updateById(conversation);
            log.info("Escalated conversation: sessionId={}", sessionId);
        });
    }

    /**
     * Get recent conversations for a user
     */
    public List<Conversation> getRecentConversations(Long userId, int limit) {
        return conversationRepository.findRecentByUserId(userId, limit);
    }

    /**
     * Get conversation statistics
     */
    public ConversationStats getStatistics() {
        List<Conversation> active = conversationRepository.findAllActive();
        long totalActive = active.size();
        
        long totalMessages = active.stream()
                .mapToLong(Conversation::getMessageCount)
                .sum();
        
        return new ConversationStats(totalActive, totalMessages);
    }

    /**
     * Simple stats record
     */
    public record ConversationStats(long activeConversations, long totalMessages) {}
}
