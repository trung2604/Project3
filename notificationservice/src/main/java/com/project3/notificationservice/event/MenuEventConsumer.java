package com.project3.notificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MenuEventConsumer {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "menu-inventory-out-of-stock", groupId = "notification-service-group")
    public void handleInventoryOutOfStock(@Payload String message) {
        try {
            log.info("Received InventoryOutOfStockEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String ingredientId = (String) event.get("ingredientId");
            String ingredientName = (String) event.get("ingredientName");
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ingredientId", ingredientId);
            metadata.put("ingredientName", ingredientName);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            String title = "Nguyên liệu hết hàng: " + ingredientName;
            String notificationMessage = String.format(
                "Nguyên liệu %s đã hết hàng. Một số món ăn có thể đã được tự động tắt.", 
                ingredientName);
            
            // Create notification for menu manager/admin
            notificationService.createNotification(
                "admin",
                "MENU_ALERT",
                title,
                notificationMessage,
                "CRITICAL",
                metadataJson
            );
            
            log.info("Created notification for inventory out of stock: {}", ingredientName);
            
        } catch (Exception e) {
            log.error("Error processing InventoryOutOfStockEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process InventoryOutOfStockEvent", e);
        }
    }
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "menu-inventory-restocked", groupId = "notification-service-group")
    public void handleInventoryRestocked(@Payload String message) {
        try {
            log.info("Received InventoryRestockedEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String ingredientId = (String) event.get("ingredientId");
            String ingredientName = (String) event.get("ingredientName");
            Object currentStockObj = event.get("currentStock");
            Object minimumStockObj = event.get("minimumStock");
            
            Double currentStock = currentStockObj instanceof Number 
                ? ((Number) currentStockObj).doubleValue() : null;
            Double minimumStock = minimumStockObj instanceof Number 
                ? ((Number) minimumStockObj).doubleValue() : null;
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ingredientId", ingredientId);
            metadata.put("ingredientName", ingredientName);
            metadata.put("currentStock", currentStock);
            metadata.put("minimumStock", minimumStock);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            String title = "Nguyên liệu đã được nhập lại: " + ingredientName;
            String notificationMessage = String.format(
                "Nguyên liệu %s đã được nhập lại. Tồn kho hiện tại: %.2f", 
                ingredientName, currentStock != null ? currentStock : 0);
            
            // Create notification for menu manager (optional, lower priority)
            notificationService.createNotification(
                "admin",
                "MENU_INFO",
                title,
                notificationMessage,
                "LOW",
                metadataJson
            );
            
            log.info("Created notification for inventory restocked: {}", ingredientName);
            
        } catch (Exception e) {
            log.error("Error processing InventoryRestockedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process InventoryRestockedEvent", e);
        }
    }
}

