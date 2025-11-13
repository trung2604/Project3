package com.project3.orderservice.command.service;

import com.project3.commonservice.dto.MenuItemIngredientsRequestEvent;
import com.project3.commonservice.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class MenuServiceClient {
    
    @Autowired
    private MenuItemIngredientsCache menuItemIngredientsCache;
    
    @Autowired
    private KafkaService kafkaService;
    
    @Value("${services.menu-service.url:http://menu-service:8002}")
    private String menuServiceUrl;
    
    private RestTemplate restTemplate;
    
    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }
        return restTemplate;
    }
    
    public List<String> getMenuItemIngredients(String menuItemId) {
        if (menuItemId == null || menuItemId.isEmpty()) {
            return List.of();
        }
        
        List<String> cached = menuItemIngredientsCache.get(menuItemId);
        if (cached != null) {
            log.debug("Menu item ingredients found in cache for menuItemId: {}", menuItemId);
            return cached;
        }
        
        List<String> ingredients = fetchMenuItemIngredientsFromService(menuItemId);
        if (ingredients != null && !ingredients.isEmpty()) {
            menuItemIngredientsCache.put(menuItemId, ingredients);
            
            MenuItemIngredientsRequestEvent request = new MenuItemIngredientsRequestEvent();
            request.setRequestId(UUID.randomUUID().toString());
            request.setMenuItemId(menuItemId);
            request.setResponseTopic("menu-item-ingredients-response");
            request.setTimestamp(System.currentTimeMillis());
            
            try {
                kafkaService.sendMessage("menu-item-ingredients-request", request);
                log.debug("Sent menu item ingredients request to Kafka for menuItemId: {}", menuItemId);
            } catch (Exception e) {
                log.warn("Failed to send menu item ingredients request to Kafka: {}", e.getMessage());
            }
        }
        
        return ingredients != null ? ingredients : List.of();
    }
    
    private List<String> fetchMenuItemIngredientsFromService(String menuItemId) {
        try {
            String url = menuServiceUrl + "/api/restaurant/menu/items/" + menuItemId;
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(
                url, HttpMethod.GET, null, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object dataObj = body.get("data");
                
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) dataObj;
                    Object ingredientsObj = data.get("ingredients");
                    
                    if (ingredientsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> ingredients = (List<String>) ingredientsObj;
                        return ingredients;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch menu item ingredients from service for menuItemId {}: {}", menuItemId, e.getMessage());
        }
        
        return List.of();
    }
}

