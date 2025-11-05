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

    @Override
    public GatewayFilter apply(JwtFilter.Config config) {
        return (exchange, chain) -> {
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

    static class Config {}
}
