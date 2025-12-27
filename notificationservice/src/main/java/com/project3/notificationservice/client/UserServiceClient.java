package com.project3.notificationservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserServiceClient {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String USER_SERVICE_NAME = "userservice";
    
    /**
     * Get base URL of User Service from Eureka
     */
    private String getUserServiceUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(USER_SERVICE_NAME);
        if (instances == null || instances.isEmpty()) {
            log.warn("User Service not found in Eureka. Using default URL.");
            return "http://localhost:8001"; // Fallback
        }
        ServiceInstance instance = instances.get(0);
        return instance.getUri().toString();
    }
    
    /**
     * Get all user IDs with a specific role
     * @param role The role to filter by (e.g., "KITCHEN_STAFF", "WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN")
     * @return List of user IDs
     */
    public List<String> getUserIdsByRole(String role) {
        try {
            String url = getUserServiceUrl() + "/api/users?role=" + role + "&size=1000";
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                
                if (data != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> users = (List<Map<String, Object>>) data.get("users");
                    
                    if (users != null) {
                        List<String> userIds = new ArrayList<>();
                        for (Map<String, Object> user : users) {
                            Object userIdObj = user.get("userId");
                            if (userIdObj != null) {
                                userIds.add(userIdObj.toString());
                            }
                        }
                        log.info("Found {} users with role {}", userIds.size(), role);
                        return userIds;
                    }
                }
            }
            
            log.warn("No users found for role {} or failed to fetch", role);
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Error fetching users by role {}: {}", role, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get user IDs for multiple roles
     * @param roles Array of roles
     * @return List of user IDs (may contain duplicates if a user has multiple roles)
     */
    public List<String> getUserIdsByRoles(String... roles) {
        List<String> allUserIds = new ArrayList<>();
        for (String role : roles) {
            List<String> userIds = getUserIdsByRole(role);
            allUserIds.addAll(userIds);
        }
        // Remove duplicates
        return allUserIds.stream().distinct().toList();
    }
}

