package com.project3.apigateway.Filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Only log errors, not all requests (reduce log noise)
        return chain.filter(exchange)
                .doOnError(error -> {
                    String path = exchange.getRequest().getURI().getPath();
                    String method = exchange.getRequest().getMethod() != null ? 
                            exchange.getRequest().getMethod().name() : "UNKNOWN";
                    log.error("Request failed: {} {} - Error: {}", method, path, error.getMessage());
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

