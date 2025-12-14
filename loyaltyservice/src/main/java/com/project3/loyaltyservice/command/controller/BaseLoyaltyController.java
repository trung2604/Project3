package com.project3.loyaltyservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.commonservice.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base controller with common functionality for loyalty controllers
 * Reduces code duplication and improves cohesion
 */
@Slf4j
public abstract class BaseLoyaltyController {
    
    /**
     * Gets current user ID from request header
     */
    protected String getCurrentUserId(HttpServletRequest request) {
        return SecurityUtils.getUserIdFromHeader(request);
    }
    
    /**
     * Creates unauthorized response
     */
    protected ResponseEntity<ApiResponseDTO<String>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponseDTO.error(message, 401));
    }
    
    /**
     * Creates bad request response
     */
    protected ResponseEntity<ApiResponseDTO<String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponseDTO.error(message, 400));
    }
}

