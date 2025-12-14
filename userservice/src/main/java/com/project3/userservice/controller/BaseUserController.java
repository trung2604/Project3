package com.project3.userservice.controller;

import com.project3.userservice.dto.ApiResponseDTO;
import com.project3.userservice.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base controller with common functionality for user controllers
 * Reduces code duplication and improves cohesion
 */
@Slf4j
public abstract class BaseUserController {
    
    /**
     * Creates unauthorized response
     */
    protected <T> ResponseEntity<ApiResponseDTO<T>> unauthorized(String message, HttpServletRequest request) {
        ErrorResponseDTO error = ErrorResponseDTO.unauthorized(message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponseDTO.error(error.getMessage(), error.getStatus()));
    }
    
    /**
     * Creates bad request response
     */
    protected <T> ResponseEntity<ApiResponseDTO<T>> badRequest(String message, HttpServletRequest request) {
        ErrorResponseDTO error = ErrorResponseDTO.badRequest(message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponseDTO.error(error.getMessage(), error.getStatus()));
    }
    
    /**
     * Creates not found response
     */
    protected <T> ResponseEntity<ApiResponseDTO<T>> notFound(String message, HttpServletRequest request) {
        ErrorResponseDTO error = ErrorResponseDTO.notFound(message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponseDTO.error(error.getMessage(), error.getStatus()));
    }
    
    /**
     * Creates forbidden response
     */
    protected <T> ResponseEntity<ApiResponseDTO<T>> forbidden(String message, HttpServletRequest request) {
        ErrorResponseDTO error = ErrorResponseDTO.forbidden(message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponseDTO.error(error.getMessage(), error.getStatus()));
    }
    
    /**
     * Handles RuntimeException and converts to appropriate HTTP response
     */
    protected <T> ResponseEntity<ApiResponseDTO<T>> handleRuntimeException(
            RuntimeException e, 
            HttpServletRequest request,
            int defaultStatusCode) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : "An error occurred";
        
        if (defaultStatusCode == 401 || errorMessage.contains("not active")) {
            int statusCode = errorMessage.contains("not active") ? 403 : 401;
            ErrorResponseDTO error = new ErrorResponseDTO(
                statusCode,
                errorMessage,
                statusCode == 403 ? "Forbidden" : "Unauthorized",
                request.getRequestURI()
            );
            return ResponseEntity.status(statusCode == 403 ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDTO.error(error.getMessage(), error.getStatus()));
        }
        
        ErrorResponseDTO error = ErrorResponseDTO.badRequest(errorMessage, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponseDTO.error(error.getMessage(), error.getStatus()));
    }
}

