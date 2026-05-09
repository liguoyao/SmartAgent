package com.smartagent.conversation.websocket;

import com.smartagent.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket authentication interceptor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            
            if (authHeaders == null || authHeaders.isEmpty()) {
                log.warn("WebSocket connection rejected: No authorization header");
                throw new IllegalArgumentException("No authorization header");
            }

            String token = authHeaders.get(0);
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (!jwtUtil.validateToken(token)) {
                log.warn("WebSocket connection rejected: Invalid token");
                throw new IllegalArgumentException("Invalid or expired token");
            }

            // Extract user info and set as session attributes
            String username = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);

            accessor.getSessionAttributes().put("username", username);
            accessor.getSessionAttributes().put("userId", userId);
            accessor.getSessionAttributes().put("role", role);

            log.info("WebSocket authenticated: user={}, userId={}", username, userId);
        }

        return message;
    }
}
