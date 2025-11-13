package com.project3.commonservice.security;

import com.project3.commonservice.dto.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

public class SecurityUtils {
    
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String USERNAME_HEADER = "X-User-Username";
    
    public static String getUserIdFromHeader(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        return userId;
    }
    
    public static String getUserIdFromHeader(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        return userIdHeader;
    }
    
    public static void validateRole(UserInfo userInfo, String... allowedRoles) {
        if (userInfo == null || userInfo.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User role not found");
        }
        
        List<String> allowedRolesList = Arrays.asList(allowedRoles);
        if (!allowedRolesList.contains(userInfo.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "User does not have required role. Required: " + Arrays.toString(allowedRoles));
        }
    }
    
    public static void validateOwnership(String resourceUserId, String currentUserId, UserInfo currentUser) {
        if (resourceUserId == null || resourceUserId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource user ID is required");
        }
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user ID is required");
        }
        
        if (currentUser == null || currentUser.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User role not found");
        }
        
        String role = currentUser.getRole();
        boolean isAdminOrManager = "ADMIN".equals(role) || "RESTAURANT_MANAGER".equals(role);
        
        if (!resourceUserId.equals(currentUserId) && !isAdminOrManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You do not have permission to access this resource");
        }
    }
    
    public static void validateOwnershipOrRole(String resourceUserId, String currentUserId, 
                                               UserInfo currentUser, String... allowedRoles) {
        if (resourceUserId == null || resourceUserId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource user ID is required");
        }
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user ID is required");
        }
        
        if (currentUser == null || currentUser.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User role not found");
        }
        
        boolean isOwner = resourceUserId.equals(currentUserId);
        boolean hasAllowedRole = Arrays.asList(allowedRoles).contains(currentUser.getRole());
        
        if (!isOwner && !hasAllowedRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You do not have permission to access this resource");
        }
    }
    
    public static boolean hasRole(UserInfo userInfo, String... roles) {
        if (userInfo == null || userInfo.getRole() == null) {
            return false;
        }
        return Arrays.asList(roles).contains(userInfo.getRole());
    }
    
    public static boolean isAdminOrManager(UserInfo userInfo) {
        return hasRole(userInfo, "ADMIN", "RESTAURANT_MANAGER");
    }
    
    public static boolean isStaffOrAbove(UserInfo userInfo) {
        return hasRole(userInfo, "STAFF", "WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN");
    }
}

