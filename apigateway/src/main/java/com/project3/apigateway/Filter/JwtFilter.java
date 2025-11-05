package com.project3.apigateway.Filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@Component
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    public JwtFilter() {
        super(Config.class);
    }

    @Override
    public String name() {
        return "jwt";
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            
            // Skip JWT processing for OAuth endpoints (no JWT token yet)
            if (path.contains("/oauth/token-exchange") || 
                path.equals("/api/users/login") || 
                path.equals("/api/users/register")) {
                return chain.filter(exchange);
            }
            
            return exchange.getPrincipal()
                    .cast(JwtAuthenticationToken.class)
                    .flatMap(jwtToken -> {
                        try {
                            var claims = jwtToken.getToken().getClaims();
                            var userId = claims.get("sub");
                            var username = claims.get("preferred_username");
                            
                            if (userId == null || username == null) {
                                log.warn("Missing required claims in JWT token");
                                return chain.filter(exchange);
                            }
                            
                            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .header("X-User-Id", userId.toString())
                                    .header("X-User-Username", username.toString())
                                    .build();
                            
                            ServerWebExchange mutatedExchange = exchange.mutate()
                                    .request(mutatedRequest)
                                    .build();
                            
                            log.debug("Added user headers: X-User-Id={}, X-User-Username={}", userId, username);
                            return chain.filter(mutatedExchange);
                        } catch (Exception e) {
                            log.error("Error processing JWT token: {}", e.getMessage());
                            return chain.filter(exchange);
                        }
                    })
                    .switchIfEmpty(chain.filter(exchange));
        };
    }

    public static class Config {
    }
}
