    package com.project3.inventoryservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base controller with common functionality for inventory controllers
 * Reduces code duplication and improves cohesion
 */
@Slf4j
public abstract class BaseInventoryController {
    
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

