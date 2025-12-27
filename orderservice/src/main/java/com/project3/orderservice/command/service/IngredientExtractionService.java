package com.project3.orderservice.command.service;

import com.project3.orderservice.command.entity.OrderItem;
import com.project3.orderservice.command.entity.OrderItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for extracting ingredient quantities from orders
 * Encapsulates ingredient extraction logic for better cohesion
 */
@Service
@Slf4j
public class IngredientExtractionService {
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private MenuServiceClient menuServiceClient;
    
    /**
     * Extracts ingredient quantities from an order
     * @param orderId The order ID
     * @return Map of ingredient ID to total quantity needed (in Double for precise calculations)
     */
    public Map<String, Double> extractIngredientQuantities(String orderId) {
        Map<String, Double> ingredientQuantities = new HashMap<>();
        
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem orderItem : orderItems) {
            // Get ingredients with quantities per serving
            Map<String, Double> ingredientsWithQuantity = 
                menuServiceClient.getMenuItemIngredientsWithQuantity(orderItem.getMenuItemId());
            
            int orderQuantity = orderItem.getQuantity(); // Number of servings ordered
            
            // Calculate total quantity needed for each ingredient
            for (Map.Entry<String, Double> entry : ingredientsWithQuantity.entrySet()) {
                String ingredientId = entry.getKey();
                Double quantityPerServing = entry.getValue(); // e.g., 0.2 kg per serving
                Double totalQuantity = quantityPerServing * orderQuantity; // e.g., 0.2 * 3 = 0.6 kg
                
                ingredientQuantities.merge(ingredientId, totalQuantity, Double::sum);
            }
        }
        
        return ingredientQuantities;
    }
}

