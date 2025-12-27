package com.project3.orderservice.command.service;

import com.project3.commonservice.dto.UserInfo;
import com.project3.commonservice.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserInfoService extends BaseHttpClientService {
    
    @Autowired
    private UserInfoCache userInfoCache;
    
    @Autowired
    private KafkaService kafkaService;
    
    @Value("${services.user-service.url:http://userservice:8005}")
    private String userServiceUrl;
    
    public UserInfo getUserInfo(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        
        UserInfo cached = userInfoCache.get(userId);
        if (cached != null) {
            log.debug("User info found in cache for userId: {}", userId);
            return cached;
        }
        
        // Try using Eureka service discovery first (if RestTemplate is @LoadBalanced)
        UserInfo userInfo = fetchUserInfoFromServiceWithDiscovery(userId);
        if (userInfo == null) {
            // Fallback to direct URL
            userInfo = fetchUserInfoFromService(userId);
        }
        if (userInfo != null) {
            userInfoCache.put(userId, userInfo);
            
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            try {
                kafkaService.sendMessage("user-info-request", request);
                log.debug("Sent user info request to Kafka for userId: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to send user info request to Kafka: {}", e.getMessage());
            }
        }
        
        return userInfo;
    }
    
    /**
     * Try to fetch user info using Eureka service discovery (if RestTemplate is @LoadBalanced)
     */
    private UserInfo fetchUserInfoFromServiceWithDiscovery(String userId) {
        try {
            // Use service name for Eureka discovery (if RestTemplate is @LoadBalanced)
            String url = "http://userservice/api/users/" + userId;
            log.debug("Trying to fetch user info via Eureka service discovery: {}", url);
            Map<String, Object> responseBody = fetchFromService(url);
            Map<String, Object> data = extractData(responseBody);
            
            if (data != null) {
                return mapToUserInfo(data);
            }
        } catch (Exception e) {
            log.debug("Eureka service discovery failed, will try direct URL: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Fallback: fetch user info using direct URL
     */
    private UserInfo fetchUserInfoFromService(String userId) {
        String url = userServiceUrl + "/api/users/" + userId;
        log.debug("Fetching user info from direct URL: {}", url);
        Map<String, Object> responseBody = fetchFromService(url);
        Map<String, Object> data = extractData(responseBody);
        
        if (data == null) {
            return null;
        }
        
        return mapToUserInfo(data);
    }
    
    private UserInfo mapToUserInfo(Map<String, Object> data) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(getStringValue(data, "userId"));
        userInfo.setUsername(getStringValue(data, "username"));
        userInfo.setEmail(getStringValue(data, "email"));
        
        Object roleObj = data.get("role");
        if (roleObj != null) {
            userInfo.setRole(roleObj.toString());
        }
        
        Object statusObj = data.get("status");
        if (statusObj != null) {
            userInfo.setStatus(statusObj.toString());
        }
        
        return userInfo;
    }
    
    /**
     * Checks if user has a specific role
     * Note: This method works with UserInfo DTO, not User entity
     * For User entity, use entity methods directly
     */
    public boolean hasRole(UserInfo userInfo, String... roles) {
        if (userInfo == null || userInfo.getRole() == null) {
            return false;
        }
        
        String userRole = userInfo.getRole().toUpperCase();
        for (String role : roles) {
            if (userRole.equals(role.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isAdminOrManager(UserInfo userInfo) {
        return hasRole(userInfo, "ADMIN", "RESTAURANT_MANAGER");
    }
    
    public boolean isStaffOrAbove(UserInfo userInfo) {
        return hasRole(userInfo, "STAFF", "KITCHEN_STAFF", "WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN");
    }
}
