package com.project3.orderservice.command.service;

import com.project3.commonservice.dto.MenuItemIngredientsRequestEvent;
import com.project3.commonservice.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class MenuServiceClient extends BaseHttpClientService {
    
    @Autowired
    private MenuItemIngredientsCache menuItemIngredientsCache;
    
    @Autowired
    private KafkaService kafkaService;
    
    @Value("${services.menu-service.url:http://menu-service:8002}")
    private String menuServiceUrl;
    
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
    
    /**
     * Gets menu item ingredients with quantities (new method)
     * Returns Map of ingredientId -> quantity per serving
     */
    public Map<String, Double> getMenuItemIngredientsWithQuantity(String menuItemId) {
        if (menuItemId == null || menuItemId.isEmpty()) {
            return Map.of();
        }
        
        Map<String, Double> ingredientQuantities = new java.util.HashMap<>();
        
        try {
            String url = menuServiceUrl + "/api/restaurant/menu/items/" + menuItemId;
            Map<String, Object> responseBody = fetchFromService(url);
            Map<String, Object> data = extractData(responseBody);
            
            if (data == null) {
                return ingredientQuantities;
            }
            
            // Try to get ingredientDetails first (new format with quantities)
            Object ingredientDetailsObj = data.get("ingredientDetails");
            if (ingredientDetailsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ingredientDetails = (List<Map<String, Object>>) ingredientDetailsObj;
                if (!ingredientDetails.isEmpty()) {
                    for (Map<String, Object> detail : ingredientDetails) {
                        String ingredientId = (String) detail.get("ingredientId");
                        Object quantityObj = detail.get("quantity");
                        if (ingredientId != null && quantityObj != null) {
                            Double quantity = quantityObj instanceof Number 
                                ? ((Number) quantityObj).doubleValue() 
                                : Double.parseDouble(quantityObj.toString());
                            ingredientQuantities.put(ingredientId, quantity);
                        }
                    }
                    log.debug("Found {} ingredients with quantities for menu item {}", 
                        ingredientQuantities.size(), menuItemId);
                    return ingredientQuantities;
                }
            }
            
            // Fallback to legacy format (simple ingredient IDs, default quantity = 1.0)
            Object ingredientsObj = data.get("ingredients");
            if (ingredientsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> ingredients = (List<String>) ingredientsObj;
                for (String ingredientId : ingredients) {
                    ingredientQuantities.put(ingredientId, 1.0); // Default: 1 unit per serving
                }
            }
        } catch (Exception e) {
            log.error("Error fetching menu item ingredients with quantity for {}: {}", menuItemId, e.getMessage());
        }
        
        return ingredientQuantities;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> fetchMenuItemIngredientsFromService(String menuItemId) {
        String url = menuServiceUrl + "/api/restaurant/menu/items/" + menuItemId;
        Map<String, Object> responseBody = fetchFromService(url);
        Map<String, Object> data = extractData(responseBody);
        
        if (data == null) {
            return List.of();
        }
        
        Object ingredientsObj = data.get("ingredients");
        if (ingredientsObj instanceof List) {
            return (List<String>) ingredientsObj;
        }
        
        return List.of();
    }
}

