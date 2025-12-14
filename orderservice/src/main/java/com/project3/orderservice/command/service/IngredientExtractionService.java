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
     * @return Map of ingredient ID to total quantity needed
     */
    public Map<String, Integer> extractIngredientQuantities(String orderId) {
        Map<String, Integer> ingredientQuantities = new HashMap<>();
        
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem orderItem : orderItems) {
            List<String> ingredients = menuServiceClient.getMenuItemIngredients(orderItem.getMenuItemId());
            int quantity = orderItem.getQuantity();
            
            for (String ingredientId : ingredients) {
                ingredientQuantities.merge(ingredientId, quantity, Integer::sum);
            }
        }
        
        return ingredientQuantities;
    }
}

