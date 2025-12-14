package com.project3.orderservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.commonservice.security.SecurityUtils;
import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.entity.OrderRespository;
import com.project3.orderservice.command.service.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base controller with common functionality for order controllers
 * Reduces code duplication and improves cohesion
 */
@Slf4j
public abstract class BaseOrderController {
    
    @Autowired
    protected OrderRespository orderRepository;
    
    @Autowired
    protected UserInfoService userInfoService;
    
    /**
     * Gets current user ID from request header
     */
    protected String getCurrentUserId(HttpServletRequest request) {
        return SecurityUtils.getUserIdFromHeader(request);
    }
    
    /**
     * Validates that user exists and is authorized
     * @return User info if valid, null otherwise
     */
    protected com.project3.commonservice.dto.UserInfo validateUser(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        var user = userInfoService.getUserInfo(userId);
        
        if (user == null) {
            log.warn("User not found: {}", userId);
        }
        
        return user;
    }
    
    /**
     * Validates that user is staff or above
     */
    protected boolean isStaffOrAbove(com.project3.commonservice.dto.UserInfo user) {
        return user != null && userInfoService.isStaffOrAbove(user);
    }
    
    /**
     * Validates that user is owner or staff
     */
    protected boolean canModifyOrder(Order order, String userId, com.project3.commonservice.dto.UserInfo user) {
        boolean isOwner = order.getCustomerId().equals(userId);
        boolean isStaffOrAbove = isStaffOrAbove(user);
        return isOwner || isStaffOrAbove;
    }
    
    /**
     * Creates unauthorized response
     */
    protected ResponseEntity<ApiResponseDTO<String>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponseDTO.error(message, 401));
    }
    
    /**
     * Creates forbidden response
     */
    protected ResponseEntity<ApiResponseDTO<String>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponseDTO.error(message, 403));
    }
    
    /**
     * Creates not found response
     */
    protected ResponseEntity<ApiResponseDTO<String>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponseDTO.error(message, 404));
    }
    
    /**
     * Creates bad request response
     */
    protected ResponseEntity<ApiResponseDTO<String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponseDTO.error(message, 400));
    }
}

