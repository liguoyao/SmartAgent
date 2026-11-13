package com.smartagent.aiservice.service;

import com.smartagent.aiservice.config.AppProperties;
import com.smartagent.aiservice.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationMemoryService {

    private final AppProperties appProperties;
    private StringRedisTemplate redisTemplate;
    private boolean redisAvailable = false;
    
    // In-memory fallback storage
    private final Map<String, List<Map<String, Object>>> inMemorySessions = new ConcurrentHashMap<>();
    
    private static final int MAX_MESSAGES = 50;
    private static final String KEY_PREFIX = "conversation:";

    public ConversationMemoryService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        try {
            // Try to initialize Redis if configured
            if (appProperties.getRedis().getHost() != null && !appProperties.getRedis().getHost().isBlank()) {
                this.redisTemplate = new StringRedisTemplate();
                // Test connection
                this.redisTemplate.getConnectionFactory().getConnection().ping();
                this.redisAvailable = true;
                log.info("Redis connection established");
            }
        } catch (Exception e) {
            log.warn("Redis not available, using in-memory storage: {}", e.getMessage());
            this.redisAvailable = false;
        }
    }

    public void addMessage(String sessionId, Message message) {
        String key = KEY_PREFIX + sessionId;
        
        if (redisAvailable) {
            addMessageToRedis(key, message);
        } else {
            addMessageToMemory(sessionId, message);
        }
    }

    private void addMessageToRedis(String key, Message message) {
        try {
            List<Map<String, Object>> messages = getMessagesFromRedis(key);
            messages.add(message.toMap());
            
            // Sliding window - trim to max messages
            if (messages.size() > MAX_MESSAGES) {
                messages = messages.subList(messages.size() - MAX_MESSAGES, messages.size());
            }
            
            redisTemplate.delete(key);
            if (!messages.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(key, 
                    messages.stream().map(this::serialize).collect(Collectors.toList()));
            }
            redisTemplate.expire(key, java.time.Duration.ofSeconds(appProperties.getRedis().getConversationTtl()));
            
            log.debug("Message added to Redis: {}", key);
        } catch (Exception e) {
            log.error("Failed to add message to Redis, falling back to memory", e);
            redisAvailable = false;
            addMessageToMemory(key.replace(KEY_PREFIX, ""), message);
        }
    }

    private void addMessageToMemory(String sessionId, Message message) {
        inMemorySessions.computeIfAbsent(sessionId, k -> new ArrayList<>())
            .add(message.toMap());
        
        // Sliding window
        List<Map<String, Object>> messages = inMemorySessions.get(sessionId);
        if (messages.size() > MAX_MESSAGES) {
            inMemorySessions.put(sessionId, new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES, messages.size())));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getMessagesFromRedis(String key) {
        List<String> rawMessages = redisTemplate.opsForList().range(key, 0, -1);
        if (rawMessages == null || rawMessages.isEmpty()) {
            return new ArrayList<>();
        }
        return rawMessages.stream().map(this::deserialize).collect(Collectors.toList());
    }

    public List<Message> getMessages(String sessionId, Integer limit) {
        String key = KEY_PREFIX + sessionId;
        List<Map<String, Object>> rawMessages;
        
        if (redisAvailable) {
            rawMessages = getMessagesFromRedis(key);
        } else {
            rawMessages = inMemorySessions.getOrDefault(sessionId, new ArrayList<>());
        }
        
        if (limit != null && rawMessages.size() > limit) {
            rawMessages = rawMessages.subList(rawMessages.size() - limit, rawMessages.size());
        }
        
        return rawMessages.stream()
            .map(this::mapToMessage)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getContextForLLM(String sessionId, Integer limit) {
        return getMessages(sessionId, limit).stream()
            .map(Message::toMap)
            .collect(Collectors.toList());
    }

    public void clear(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        if (redisAvailable) {
            redisTemplate.delete(key);
        }
        inMemorySessions.remove(sessionId);
        log.info("Session cleared: {}", sessionId);
    }

    public void extendTtl(String sessionId) {
        if (redisAvailable) {
            String key = KEY_PREFIX + sessionId;
            redisTemplate.expire(key, java.time.Duration.ofSeconds(appProperties.getRedis().getConversationTtl()));
        }
    }

    public int getSessionCount() {
        if (redisAvailable) {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        }
        return inMemorySessions.size();
    }

    public boolean healthCheck() {
        if (redisAvailable) {
            try {
                return redisTemplate.getConnectionFactory().getConnection().ping() != null;
            } catch (Exception e) {
                log.error("Redis health check failed", e);
                return false;
            }
        }
        return true; // In-memory is always healthy
    }

    private String serialize(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserialize(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Message mapToMessage(Map<String, Object> map) {
        return Message.builder()
            .role((String) map.get("role"))
            .content((String) map.get("content"))
            .toolCalls((List<Map<String, Object>>) map.get("tool_calls"))
            .toolCallId((String) map.get("tool_call_id"))
            .name((String) map.get("name"))
            .timestamp(map.get("timestamp") != null ? (String) map.get("timestamp") : java.time.Instant.now().toString())
            .build();
    }
}
