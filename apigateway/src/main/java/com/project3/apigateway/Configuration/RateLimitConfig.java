package com.project3.apigateway.Configuration;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    
    /**
     * User-based Rate Limiter
     * Rate limit theo userId từ JWT token
     * Mỗi user có quota riêng
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Lấy userId từ header (được set bởi JwtFilter)
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            
            // Fallback: Nếu không có userId, dùng IP
            String ip = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            
            return Mono.just(ip);
        };
    }
    
    /**
     * IP-based Rate Limiter
     * Rate limit theo IP address
     * Dùng cho public endpoints
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            return Mono.just(ip);
        };
    }
    
    /**
     * API Key-based Rate Limiter (Optional)
     * Rate limit theo API key
     * Dùng cho B2B integration
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            
            if (apiKey != null && !apiKey.isEmpty()) {
                return Mono.just(apiKey);
            }
            
            // Fallback to IP
            String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            
            return Mono.just(ip);
        };
    }
}

