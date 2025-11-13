package com.project3.commonservice.exception;

import com.project3.commonservice.dto.ApiResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all microservices.
 * This handler provides consistent error responses across all services.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed for {}: {}", request.getRequestURI(), errors);
        
        ApiResponseDTO<Map<String, String>> response = ApiResponseDTO.badRequest("Validation failed");
        response.setData(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle date/time parsing errors
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleDateTimeParseException(
            DateTimeParseException ex,
            HttpServletRequest request) {
        log.error("Invalid date format for {}: {}", request.getRequestURI(), ex.getMessage());
        
        String message = "Invalid date format. Use ISO format: yyyy-MM-ddTHH:mm:ss";
        ApiResponseDTO<Void> response = ApiResponseDTO.badRequest(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.error("Illegal argument exception for {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiResponseDTO<Void> response = ApiResponseDTO.badRequest(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {
        log.error("Illegal state exception for {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiResponseDTO<Void> response = ApiResponseDTO.badRequest(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle null pointer exceptions
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleNullPointerException(
            NullPointerException ex,
            HttpServletRequest request) {
        log.error("Null pointer exception for {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiResponseDTO<Void> response = ApiResponseDTO.internalServerError(
            "A null reference was encountered. Please check the request data.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unexpected error for {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiResponseDTO<Void> response = ApiResponseDTO.internalServerError(
            "An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

