package com.project3.menuservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Supplier;

/**
 * Base controller with common functionality for menu controllers
 * Reduces code duplication and improves cohesion
 */
@Slf4j
public abstract class BaseMenuController {
    
    /**
     * Handles aggregate not found exception with fallback logic
     */
    protected <T> ResponseEntity<ApiResponseDTO<T>> handleAggregateNotFoundFallback(
            String id, 
            Supplier<ResponseEntity<ApiResponseDTO<T>>> fallback) {
        try {
            return fallback.get();
        } catch (Exception e) {
            log.error("Error in fallback handler for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to sync: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Checks if exception is AggregateNotFoundException
     */
    protected boolean isAggregateNotFound(Exception e) {
        if (e instanceof AggregateNotFoundException) {
            return true;
        }
        
        String errorMessage = e.getMessage();
        Throwable cause = e.getCause();
        String causeMessage = cause != null ? cause.getMessage() : null;
        
        return (errorMessage != null && 
            (errorMessage.toLowerCase().contains("aggregate") && errorMessage.toLowerCase().contains("not found"))) ||
            (causeMessage != null && 
            (causeMessage.toLowerCase().contains("aggregate") && causeMessage.toLowerCase().contains("not found")));
    }
    
    /**
     * Creates bad request response
     */
    protected <T> ResponseEntity<ApiResponseDTO<T>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponseDTO.error(message, 400));
    }
    
    /**
     * Creates not found response
     */
    protected <T> ResponseEntity<ApiResponseDTO<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponseDTO.error(message, 404));
    }
}

