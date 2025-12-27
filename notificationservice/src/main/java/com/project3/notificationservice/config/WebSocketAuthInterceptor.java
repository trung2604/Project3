package com.project3.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;

/**
 * WebSocket authentication interceptor
 * Extracts user ID from query parameter or header and sets it as principal
 */
@Component
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor, ChannelInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            // Get user ID from query parameter (e.g., ws://localhost:8081/ws/notifications?userId=xxx)
            String userId = httpRequest.getParameter("userId");
            
            if (userId != null && !userId.isEmpty()) {
                // Store user ID in attributes for later use
                attributes.put("userId", userId);
                
                // Create a simple principal with user ID
                Principal principal = () -> userId;
                attributes.put("principal", principal);
                
                log.info("WebSocket handshake for user: {}", userId);
                return true;
            } else {
                log.warn("WebSocket handshake rejected: no userId parameter");
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Get user ID from session attributes
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                String userId = (String) sessionAttributes.get("userId");
                if (userId != null) {
                    // Create a simple principal with user ID
                    Principal principal = () -> userId;
                    accessor.setUser(principal);
                    log.info("WebSocket connection authenticated for user: {}", userId);
                }
            }
        }
        
        return message;
    }
}

