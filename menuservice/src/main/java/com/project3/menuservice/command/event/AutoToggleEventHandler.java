package com.project3.menuservice.command.event;

import com.project3.commonservice.service.KafkaService;
import com.project3.menuservice.command.entity.MenuItem;
import com.project3.menuservice.command.entity.MenuItemRepository;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AutoToggleEventHandler {

    @Autowired
    private MenuItemRepository menuItemRepository;
    
    @Autowired(required = false)
    private KafkaService kafkaService;

    @EventHandler
    public void on(InventoryLowEvent event) {
        // Find all menu items that use this ingredient
        List<MenuItem> affectedItems = menuItemRepository.findAll().stream()
            .filter(item -> item.getIngredients().contains(event.getIngredientId()) || 
                           item.getIngredients().contains(event.getIngredientName()))
            .filter(item -> item.getActive()) // Only toggle active items
            .toList();

        // Toggle items to inactive if stock is below minimum
        if (event.getCurrentStock() < event.getMinimumStock()) {
            affectedItems.forEach(item -> {
                item.setActive(false);
                menuItemRepository.save(item);
            });
        }
    }

    @EventHandler
    public void on(InventoryOutOfStockEvent event) {
        // Find all menu items that use this ingredient and disable them
        List<MenuItem> affectedItems = menuItemRepository.findAll().stream()
            .filter(item -> item.getIngredients().contains(event.getIngredientId()) || 
                           item.getIngredients().contains(event.getIngredientName()))
            .filter(item -> item.getActive()) // Only toggle active items
            .toList();

        affectedItems.forEach(item -> {
            item.setActive(false);
            menuItemRepository.save(item);
        });
        
        // Send to Kafka for Notification Service
        if (kafkaService != null && !affectedItems.isEmpty()) {
            try {
                kafkaService.sendMessage("menu-inventory-out-of-stock", event);
            } catch (Exception e) {
                // Log error but don't fail the event handler
                System.err.println("Failed to send InventoryOutOfStockEvent to Kafka: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void on(InventoryRestockedEvent event) {
        // Find all menu items that use this ingredient
        List<MenuItem> affectedItems = menuItemRepository.findAll().stream()
            .filter(item -> item.getIngredients().contains(event.getIngredientId()) || 
                           item.getIngredients().contains(event.getIngredientName()))
            .filter(item -> !item.getActive()) // Only check inactive items
            .toList();

        // Check if all ingredients for these items are now available
        affectedItems.forEach(item -> {
            boolean allIngredientsAvailable = checkAllIngredientsAvailable(item);
            if (allIngredientsAvailable) {
                item.setActive(true);
                menuItemRepository.save(item);
            }
        });
        
        // Send to Kafka for Notification Service (optional, lower priority)
        if (kafkaService != null) {
            try {
                kafkaService.sendMessage("menu-inventory-restocked", event);
            } catch (Exception e) {
                // Log error but don't fail the event handler
                System.err.println("Failed to send InventoryRestockedEvent to Kafka: " + e.getMessage());
            }
        }
    }

    private boolean checkAllIngredientsAvailable(MenuItem item) {
        // This is a simplified check - in a real system, you'd query the inventory service
        // to check if all ingredients for this item are available
        // For now, we'll assume ingredients are available if they're not in a "low stock" state
        return true; // Simplified implementation
    }
}
