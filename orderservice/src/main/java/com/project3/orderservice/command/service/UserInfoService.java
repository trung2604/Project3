package com.project3.orderservice.command.service;

import com.project3.commonservice.dto.UserInfo;
import com.project3.commonservice.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserInfoService {
    
    @Autowired
    private UserInfoCache userInfoCache;
    
    @Autowired
    private KafkaService kafkaService;
    
    @Value("${services.user-service.url:http://user-service:8080}")
    private String userServiceUrl;
    
    private RestTemplate restTemplate;
    
    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }
        return restTemplate;
    }
    
    public UserInfo getUserInfo(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        
        UserInfo cached = userInfoCache.get(userId);
        if (cached != null) {
            log.debug("User info found in cache for userId: {}", userId);
            return cached;
        }
        
        UserInfo userInfo = fetchUserInfoFromService(userId);
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
    
    private UserInfo fetchUserInfoFromService(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(
                url, HttpMethod.GET, null, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object dataObj = body.get("data");
                
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) dataObj;
                    
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
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user info from service for userId {}: {}", userId, e.getMessage());
        }
        
        return null;
    }
    
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
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
        return hasRole(userInfo, "STAFF", "WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN");
    }
}
