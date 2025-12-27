package com.project3.notificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.notificationservice.client.UserServiceClient;
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
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class InventoryEventConsumer {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "inventory-low-stock-alert", groupId = "notification-service-group")
    public void handleLowStockAlert(@Payload String message) {
        try {
            log.info("Received LowStockAlertEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String ingredientId = (String) event.get("ingredientId");
            String ingredientName = (String) event.get("ingredientName");
            Object currentStockObj = event.get("currentStock");
            Object minStockLevelObj = event.get("minStockLevel");
            String severity = (String) event.get("severity");
            String alertMessage = (String) event.get("message");
            
            Double currentStock = currentStockObj instanceof Number 
                ? ((Number) currentStockObj).doubleValue() : null;
            Double minStockLevel = minStockLevelObj instanceof Number 
                ? ((Number) minStockLevelObj).doubleValue() : null;
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ingredientId", ingredientId);
            metadata.put("ingredientName", ingredientName);
            metadata.put("currentStock", currentStock);
            metadata.put("minStockLevel", minStockLevel);
            metadata.put("severity", severity);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            String title = "Cảnh báo tồn kho thấp: " + ingredientName;
            String notificationMessage = alertMessage != null ? alertMessage 
                : String.format("Nguyên liệu %s đang có tồn kho thấp. Hiện tại: %.2f, Tối thiểu: %.2f", 
                    ingredientName, currentStock != null ? currentStock : 0, 
                    minStockLevel != null ? minStockLevel : 0);
            
            // Notify warehouse staff, managers, and admins
            List<String> userIds = userServiceClient.getUserIdsByRoles(
                "WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN");
            
            for (String userId : userIds) {
                notificationService.createNotification(
                    userId,
                    "INVENTORY_ALERT",
                    title,
                    notificationMessage,
                    severity != null ? severity : "HIGH",
                    metadataJson
                );
            }
            
            log.info("Created notification for low stock alert: {} to {} users", ingredientName, userIds.size());
            
        } catch (Exception e) {
            log.error("Error processing LowStockAlertEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process LowStockAlertEvent", e);
        }
    }
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "inventory-expiry-alert", groupId = "notification-service-group")
    public void handleExpiryAlert(@Payload String message) {
        try {
            log.info("Received ExpiryAlertEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String ingredientId = (String) event.get("ingredientId");
            String ingredientName = (String) event.get("ingredientName");
            String expiryDate = (String) event.get("expiryDate");
            String severity = (String) event.get("severity");
            String alertMessage = (String) event.get("message");
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ingredientId", ingredientId);
            metadata.put("ingredientName", ingredientName);
            metadata.put("expiryDate", expiryDate);
            metadata.put("severity", severity);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            String title = "Cảnh báo hết hạn: " + ingredientName;
            String notificationMessage = alertMessage != null ? alertMessage 
                : String.format("Nguyên liệu %s sẽ hết hạn vào ngày %s", ingredientName, expiryDate);
            
            // Notify warehouse staff, managers, and admins
            List<String> userIds = userServiceClient.getUserIdsByRoles(
                "WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN");
            
            for (String userId : userIds) {
                notificationService.createNotification(
                    userId,
                    "INVENTORY_ALERT",
                    title,
                    notificationMessage,
                    severity != null ? severity : "HIGH",
                    metadataJson
                );
            }
            
            log.info("Created notification for expiry alert: {} to {} users", ingredientName, userIds.size());
            
        } catch (Exception e) {
            log.error("Error processing ExpiryAlertEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process ExpiryAlertEvent", e);
        }
    }
}

