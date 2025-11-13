package com.project3.menuservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.commonservice.dto.MenuItemIngredientsRequestEvent;
import com.project3.commonservice.dto.MenuItemIngredientsResponseEvent;
import com.project3.commonservice.service.KafkaService;
import com.project3.menuservice.command.entity.MenuItem;
import com.project3.menuservice.command.entity.MenuItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class MenuItemIngredientsKafkaConsumer {
    
    @Autowired
    private MenuItemRepository menuItemRepository;
    
    @Autowired
    private KafkaService kafkaService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "menu-item-ingredients-request", groupId = "menu-service-group")
    public void handleMenuItemIngredientsRequest(@Payload String message) {
        try {
            log.info("Received menu item ingredients request: {}", message);
            
            MenuItemIngredientsRequestEvent request = objectMapper.readValue(message, MenuItemIngredientsRequestEvent.class);
            
            MenuItem menuItem = menuItemRepository.findWithDetailsById(request.getMenuItemId()).orElse(null);
            if (menuItem == null) {
                log.warn("Menu item not found: {}", request.getMenuItemId());
                return;
            }
            
            List<String> ingredients = new ArrayList<>(menuItem.getIngredients());
            
            MenuItemIngredientsResponseEvent response = new MenuItemIngredientsResponseEvent();
            response.setRequestId(request.getRequestId() != null ? request.getRequestId() : UUID.randomUUID().toString());
            response.setMenuItemId(request.getMenuItemId());
            response.setIngredientIds(ingredients);
            response.setTimestamp(System.currentTimeMillis());
            
            String responseTopic = request.getResponseTopic() != null ? 
                request.getResponseTopic() : "menu-item-ingredients-response";
            
            kafkaService.sendMessage(responseTopic, response);
            log.info("Published menu item ingredients response for menu item: {}", request.getMenuItemId());
            
        } catch (Exception e) {
            log.error("Error processing menu item ingredients request: {}", e.getMessage(), e);
        }
    }
}

