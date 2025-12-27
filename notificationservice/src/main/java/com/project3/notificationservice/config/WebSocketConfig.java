package com.project3.notificationservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time notifications
 * Uses STOMP protocol over WebSocket
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor authInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to send messages to clients
        // Clients subscribe to destinations prefixed with "/topic" or "/user"
        config.enableSimpleBroker("/topic", "/user");
        
        // Messages from clients to server should be prefixed with "/app"
        config.setApplicationDestinationPrefixes("/app");
        
        // User-specific destinations (e.g., /user/{userId}/notifications)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint that clients will connect to
        // SockJS fallback is enabled for browsers that don't support WebSocket
        registry.addEndpoint("/ws/notifications")
                .addInterceptors(authInterceptor) // Add authentication interceptor
                .setAllowedOriginPatterns("http://localhost:5173", "http://localhost:3000") // Specific origins to avoid CORS duplicate
                .withSockJS(); // Enable SockJS fallback options
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add interceptor to authenticate WebSocket connections
        registration.interceptors(authInterceptor);
    }
}

