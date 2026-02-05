package com.project3.paymentservice.command.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Base service for HTTP client operations
 * Encapsulates common HTTP client logic to reduce duplication
 */
@Slf4j
public abstract class BaseHttpClientService {
    
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    protected RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new org.springframework.web.client.RestTemplate();
            log.warn("RestTemplate not injected, creating default instance. Consider using @LoadBalanced RestTemplate for service discovery.");
        }
        return restTemplate;
    }
    
    /**
     * Fetches data from external service
     * @param url The service URL
     * @return Response body as Map, or null if failed
     */
    protected Map<String, Object> fetchFromService(String url) {
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(
                url, HttpMethod.GET, null, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch from service URL {}: {}", url, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extracts data object from API response
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> extractData(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return null;
        }
        
        Object dataObj = responseBody.get("data");
        if (dataObj instanceof Map) {
            return (Map<String, Object>) dataObj;
        }
        
        return null;
    }
    
    /**
     * Gets string value from map safely
     */
    protected String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
