package com.project3.inventoryservice.command.event;

import com.project3.inventoryservice.command.entity.*;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.project3.inventoryservice.service.CloudinaryService;


@Component
public class InventoryEventHandler {
    
    @Autowired
    private IngredientRepository ingredientRepository;
    
    @Autowired
    private StockTransactionRepository stockTransactionRepository;
    
    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Autowired(required = false)
    private CloudinaryService cloudinaryService;
    
    @Autowired
    private com.project3.inventoryservice.command.service.StockTransactionMapper transactionMapper;
    
    @Autowired
    private com.project3.inventoryservice.command.service.InventoryKafkaPublisher kafkaPublisher;
    
    @EventHandler
    public void on(IngredientCreatedEvent event) {
        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientId(event.getIngredientId());
        ingredient.setName(event.getName());
        ingredient.setDescription(event.getDescription());
        ingredient.setUnit(event.getUnit());
        ingredient.updateStock(event.getInitialStock()); // Use entity method
        ingredient.setMinStockLevel(event.getMinStockLevel());
        ingredient.setMaxStockLevel(event.getMaxStockLevel());
        ingredient.setExpiryDate(event.getExpiryDate());
        ingredient.setActive(true); // New ingredient is active by default
        ingredient.setCreatedAt(event.getCreatedAt());
        ingredient.setSupplierName(event.getSupplierName());
        ingredient.setSupplierContact(event.getSupplierContact());
        ingredient.setUnitCost(event.getUnitCost());
        ingredient.setCurrency(event.getCurrency());
        ingredient.setCategory(event.getCategory());
        ingredient.setImageUrl(event.getImageUrl());
        ingredient.setImagePublicId(event.getImagePublicId());
        
        ingredientRepository.save(ingredient);
        
        // Create initial stock transaction
        StockTransaction transaction = transactionMapper.createInitialStockTransaction(
            event.getIngredientId(), 
            event.getInitialStock(), 
            event.getUnit(), 
            event.getCreatedAt()
        );
        stockTransactionRepository.save(transaction);
    }
    
    @EventHandler
    public void on(IngredientUpdatedEvent event) {
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.setName(event.getName());
            ingredient.setDescription(event.getDescription());
            ingredient.setUnit(event.getUnit());
            ingredient.setMinStockLevel(event.getMinStockLevel());
            ingredient.setMaxStockLevel(event.getMaxStockLevel());
            ingredient.setExpiryDate(event.getExpiryDate());
            ingredient.setUpdatedAt(event.getUpdatedAt());
            ingredient.setSupplierName(event.getSupplierName());
            ingredient.setSupplierContact(event.getSupplierContact());
            ingredient.setUnitCost(event.getUnitCost());
            ingredient.setCurrency(event.getCurrency());
            ingredient.setCategory(event.getCategory());
        ingredient.setImageUrl(event.getImageUrl());
        ingredient.setImagePublicId(event.getImagePublicId());
            
            ingredientRepository.save(ingredient);
        }
    }
    
    @EventHandler
    public void on(IngredientDeletedEvent event) {
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            if (cloudinaryService != null && ingredient.getImagePublicId() != null && !ingredient.getImagePublicId().isEmpty()) {
                cloudinaryService.deleteImage(ingredient.getImagePublicId());
            }
            ingredient.deactivate(); // Soft delete using entity method
            ingredientRepository.save(ingredient);
        }
    }
    
    @EventHandler
    public void on(IngredientToggledEvent event) {
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            if (event.getActive()) {
                ingredient.setActive(true);
            } else {
                ingredient.deactivate();
            }
            ingredientRepository.save(ingredient);
        }
    }
    
    @EventHandler
    public void on(StockInEvent event) {
        // Update ingredient stock using entity method
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.updateStock(event.getStockAfter());
            ingredientRepository.save(ingredient);
        }
        
        // Create stock transaction record
        StockTransaction transaction = transactionMapper.fromStockInEvent(event);
        stockTransactionRepository.save(transaction);
    }
    
    @EventHandler
    public void on(StockOutEvent event) {
        // Update ingredient stock using entity method
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.updateStock(event.getStockAfter());
            ingredientRepository.save(ingredient);
        }
        
        // Create stock transaction record
        StockTransaction transaction = transactionMapper.fromStockOutEvent(event);
        stockTransactionRepository.save(transaction);
    }
    
    @EventHandler
    public void on(StockAdjustedEvent event) {
        // Update ingredient stock using entity method
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.updateStock(event.getStockAfter());
            ingredientRepository.save(ingredient);
        }
        
        // Create stock transaction record
        StockTransaction transaction = transactionMapper.fromStockAdjustedEvent(event);
        stockTransactionRepository.save(transaction);
    }
    
    @EventHandler
    public void on(StockTakenEvent event) {
        // Update ingredient stock using entity method
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.updateStock(event.getStockAfter());
            ingredientRepository.save(ingredient);
        }
        
        // Create stock transaction record
        StockTransaction transaction = transactionMapper.fromStockTakenEvent(event);
        stockTransactionRepository.save(transaction);
    }
    
    @EventHandler
    public void on(LowStockAlertEvent event) {
        StockAlert alert = new StockAlert();
        alert.setAlertId(event.getAlertId());
        alert.setIngredientId(event.getIngredientId());
        alert.setAlertType("LOW_STOCK");
        alert.setSeverity(event.getSeverity());
        alert.setMessage(event.getMessage());
        alert.setAlertDate(event.getAlertDate());
        alert.setIsActive(true);
        alert.setCurrentStock(event.getCurrentStock());
        alert.setMinStockLevel(event.getMinStockLevel());
        
        stockAlertRepository.save(alert);
        
        // Send to Kafka for Notification Service
        kafkaPublisher.publishLowStockAlert(event);
    }
    
    @EventHandler
    public void on(ExpiryAlertEvent event) {
        StockAlert alert = new StockAlert();
        alert.setAlertId(event.getAlertId());
        alert.setIngredientId(event.getIngredientId());
        alert.setAlertType("EXPIRY");
        alert.setSeverity(event.getSeverity());
        alert.setMessage(event.getMessage());
        alert.setAlertDate(event.getAlertDate());
        alert.setIsActive(true);
        alert.setExpiryDate(event.getExpiryDate());
        
        stockAlertRepository.save(alert);
        
        // Send to Kafka for Notification Service
        kafkaPublisher.publishExpiryAlert(event);
    }
}
