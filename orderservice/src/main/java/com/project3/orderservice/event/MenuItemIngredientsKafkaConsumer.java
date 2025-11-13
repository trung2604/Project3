package com.project3.orderservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.commonservice.dto.MenuItemIngredientsResponseEvent;
import com.project3.orderservice.command.service.MenuItemIngredientsCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MenuItemIngredientsKafkaConsumer {
    
    @Autowired
    private MenuItemIngredientsCache menuItemIngredientsCache;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "menu-item-ingredients-response", groupId = "order-service-group")
    public void handleMenuItemIngredientsResponse(@Payload String message) {
        try {
            log.debug("Received menu item ingredients response: {}", message);
            
            MenuItemIngredientsResponseEvent event = objectMapper.readValue(message, MenuItemIngredientsResponseEvent.class);
            
            menuItemIngredientsCache.put(event.getMenuItemId(), event.getIngredientIds());
            log.debug("Updated ingredients cache for menu item: {}", event.getMenuItemId());
            
        } catch (Exception e) {
            log.error("Error processing menu item ingredients response: {}", e.getMessage(), e);
        }
    }
}

