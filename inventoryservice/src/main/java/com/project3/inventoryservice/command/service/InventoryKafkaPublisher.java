package com.project3.inventoryservice.command.service;

import com.project3.commonservice.service.KafkaService;
import com.project3.inventoryservice.command.event.ExpiryAlertEvent;
import com.project3.inventoryservice.command.event.LowStockAlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for publishing inventory-related Kafka events
 * Encapsulates all Kafka event publishing logic for better cohesion
 */
@Service
@Slf4j
public class InventoryKafkaPublisher {
    
    @Autowired(required = false)
    private KafkaService kafkaService;
    
    /**
     * Publishes low stock alert event
     */
    public void publishLowStockAlert(LowStockAlertEvent event) {
        if (kafkaService == null) {
            return;
        }
        
        try {
            kafkaService.sendMessage("inventory-low-stock-alert", event);
        } catch (Exception e) {
            log.error("Failed to send LowStockAlertEvent to Kafka: {}", e.getMessage());
        }
    }
    
    /**
     * Publishes expiry alert event
     */
    public void publishExpiryAlert(ExpiryAlertEvent event) {
        if (kafkaService == null) {
            return;
        }
        
        try {
            kafkaService.sendMessage("inventory-expiry-alert", event);
        } catch (Exception e) {
            log.error("Failed to send ExpiryAlertEvent to Kafka: {}", e.getMessage());
        }
    }
}

