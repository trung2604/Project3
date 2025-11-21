package com.project3.apigateway.Filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Order(-1)
@Component
public class RateLimitExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // Handle rate limit exceeded
        if (ex.getMessage() != null && ex.getMessage().contains("429")) {
            return handleRateLimitExceeded(exchange, response);
        }
        
        // Handle service not found
        if (ex instanceof NotFoundException) {
            return handleServiceNotFound(exchange, response);
        }
        
        // Handle other errors
        return handleGenericError(exchange, response, ex);
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", 429);
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", "You have exceeded the rate limit. Please try again later.");
        errorResponse.put("path", exchange.getRequest().getPath().value());
        
        // Add Retry-After header (60 seconds)
        response.getHeaders().add("Retry-After", "60");
        response.getHeaders().add("X-RateLimit-Limit", "100");
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000));
        
        log.warn("Rate limit exceeded for request: {} from IP: {}", 
            exchange.getRequest().getPath(),
            exchange.getRequest().getRemoteAddress());
        
        return writeResponse(response, errorResponse);
    }

    private Mono<Void> handleServiceNotFound(ServerWebExchange exchange, ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", 503);
        errorResponse.put("error", "Service Unavailable");
        errorResponse.put("message", "Service is temporarily unavailable. Please try again later.");
        errorResponse.put("path", exchange.getRequest().getPath().value());
        
        log.error("Service not found for request: {}", exchange.getRequest().getPath());
        
        return writeResponse(response, errorResponse);
    }

    private Mono<Void> handleGenericError(ServerWebExchange exchange, ServerHttpResponse response, Throwable ex) {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", 500);
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An unexpected error occurred");
        errorResponse.put("path", exchange.getRequest().getPath().value());
        
        log.error("Error processing request: {}", exchange.getRequest().getPath(), ex);
        
        return writeResponse(response, errorResponse);
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, Map<String, Object> body) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing response", e);
            return response.setComplete();
        }
    }
}

