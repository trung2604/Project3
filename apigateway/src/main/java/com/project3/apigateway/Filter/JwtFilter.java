package com.project3.apigateway.Filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
            
            // Try to get Principal (JWT token should be validated by Spring Security first)
            // This filter runs AFTER Spring Security authentication
            return exchange.getPrincipal()
                    .cast(JwtAuthenticationToken.class)
                    .map(jwtToken -> {
                        try {
                            var claims = jwtToken.getToken().getClaims();
                            var userId = claims.get("sub");
                            var username = claims.get("preferred_username");
                            
                            if (userId == null || username == null) {
                                log.warn("Missing required claims in JWT token for path: {}", path);
                                return exchange;
                            }
                            
                            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .header("X-User-Id", userId.toString())
                                    .header("X-User-Username", username.toString())
                                    .build();
                            
                            ServerWebExchange mutatedExchange = exchange.mutate()
                                    .request(mutatedRequest)
                                    .build();
                            
                            log.debug("Added user headers: X-User-Id={}, X-User-Username={} for path: {}", 
                                    userId, username, path);
                            return mutatedExchange;
                        } catch (Exception e) {
                            log.error("Error processing JWT token for path {}: {}", path, e.getMessage(), e);
                            return exchange;
                        }
                    })
                    .switchIfEmpty(
                        Mono.defer(() -> {
                            // If no Principal, log warning but continue
                            // This might happen if request is rejected by Spring Security
                            log.warn("No Principal found for authenticated path: {}. " +
                                    "This might indicate a security configuration issue.", path);
                            return Mono.just(exchange);
                        })
                    )
                    .flatMap(chain::filter);
        };
    }

    public static class Config {
    }
}
