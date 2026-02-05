package com.project3.paymentservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.commonservice.dto.UserInfo;
import com.project3.commonservice.security.SecurityUtils;
import com.project3.paymentservice.command.entity.Payment;
import com.project3.paymentservice.command.entity.PaymentRepository;
import com.project3.paymentservice.command.service.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base controller with security and common functionality for payment controllers
 */
@Slf4j
public abstract class BasePaymentController {
    
    @Autowired
    protected PaymentRepository paymentRepository;
    
    @Autowired
    protected UserInfoService userInfoService;
    
    /**
     * Gets current user ID from request header (set by API Gateway JWT filter)
     */
    protected String getCurrentUserId(HttpServletRequest request) {
        return SecurityUtils.getUserIdFromHeader(request);
    }
    
    /**
     * Validates that user exists and retrieves user info
     * @return User info if valid, null otherwise
     */
    protected UserInfo validateUser(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        UserInfo user = userInfoService.getUserInfo(userId);
        
        if (user == null) {
            log.warn("User not found: {}", userId);
        }
        
        return user;
    }
    
    /**
     * Checks if user has STAFF, KITCHEN_STAFF, MANAGER, or ADMIN role
     */
    protected boolean isStaffOrAbove(UserInfo user) {
        return user != null && userInfoService.isStaffOrAbove(user);
    }
    
    /**
     * Checks if user has ADMIN or MANAGER role
     */
    protected boolean isAdminOrManager(UserInfo user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String role = user.getRole().toUpperCase();
        return role.equals("RESTAURANT_MANAGER") || role.equals("ADMIN");
    }
    
    /**
     * Checks if user can access a payment
     * - Payment owner can access
     * - STAFF and above can access any payment
     */
    protected boolean canAccessPayment(Payment payment, String userId, UserInfo user) {
        if (payment == null) {
            return false;
        }
        
        // Payment owner can access
        if (payment.getCustomerId() != null && payment.getCustomerId().equals(userId)) {
            return true;
        }
        
        // Staff and above can access any payment
        return isStaffOrAbove(user);
    }
    
    /**
     * Checks if user can refund a payment
     * Only ADMIN and MANAGER can refund payments
     */
    protected boolean canRefundPayment(UserInfo user) {
        return isAdminOrManager(user);
    }
    
    /**
     * Checks if user can create payment for a specific customer
     * - User can create for themselves
     * - STAFF+ can create for anyone
     */
    protected boolean canCreatePaymentFor(String targetCustomerId, String currentUserId, UserInfo currentUser) {
        // Can create for self
        if (targetCustomerId.equals(currentUserId)) {
            return true;
        }
        
        // STAFF+ can create for anyone
        return isStaffOrAbove(currentUser);
    }
    
    // Response helpers
    
    protected <T> ResponseEntity<ApiResponseDTO<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponseDTO.error(message, 401));
    }
    
    protected <T> ResponseEntity<ApiResponseDTO<T>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponseDTO.error(message, 403));
    }
    
    protected <T> ResponseEntity<ApiResponseDTO<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponseDTO.error(message, 404));
    }
    
    protected <T> ResponseEntity<ApiResponseDTO<T>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponseDTO.error(message, 400));
    }
}
