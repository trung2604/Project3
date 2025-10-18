package com.project3.inventoryservice.command.event;

import com.project3.inventoryservice.command.entity.*;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InventoryEventHandler {
    
    @Autowired
    private IngredientRepository ingredientRepository;
    
    @Autowired
    private StockTransactionRepository stockTransactionRepository;
    
    @Autowired
    private StockAlertRepository stockAlertRepository;
    
    @EventHandler
    public void on(IngredientCreatedEvent event) {
        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientId(event.getIngredientId());
        ingredient.setName(event.getName());
        ingredient.setDescription(event.getDescription());
        ingredient.setUnit(event.getUnit());
        ingredient.setCurrentStock(event.getInitialStock());
        ingredient.setMinStockLevel(event.getMinStockLevel());
        ingredient.setMaxStockLevel(event.getMaxStockLevel());
        ingredient.setExpiryDate(event.getExpiryDate());
        ingredient.setActive(true);
        ingredient.setCreatedAt(event.getCreatedAt());
        ingredient.setSupplierName(event.getSupplierName());
        ingredient.setSupplierContact(event.getSupplierContact());
        ingredient.setUnitCost(event.getUnitCost());
        ingredient.setCurrency(event.getCurrency());
        ingredient.setCategory(event.getCategory());
        
        ingredientRepository.save(ingredient);
        
        // Create initial stock transaction
        StockTransaction transaction = new StockTransaction();
        transaction.setTransactionId("init-" + event.getIngredientId());
        transaction.setIngredientId(event.getIngredientId());
        transaction.setTransactionType("INITIAL_STOCK");
        transaction.setQuantity(event.getInitialStock());
        transaction.setUnit(event.getUnit());
        transaction.setTransactionDate(event.getCreatedAt());
        transaction.setReference("Initial Stock");
        transaction.setCreatedBy("SYSTEM");
        transaction.setStockBefore(0.0);
        transaction.setStockAfter(event.getInitialStock());
        
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
            
            ingredientRepository.save(ingredient);
        }
    }
    
    @EventHandler
    public void on(IngredientDeletedEvent event) {
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.setActive(false);
            ingredientRepository.save(ingredient);
        }
    }
    
    @EventHandler
    public void on(IngredientToggledEvent event) {
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.setActive(event.getActive());
            ingredientRepository.save(ingredient);
        }
    }
    
    @EventHandler
    public void on(StockInEvent event) {
        // Update ingredient stock
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.setCurrentStock(event.getStockAfter());
            ingredientRepository.save(ingredient);
        }
        
        // Create stock transaction record
        StockTransaction transaction = new StockTransaction();
        transaction.setTransactionId(event.getTransactionId());
        transaction.setIngredientId(event.getIngredientId());
        transaction.setTransactionType("STOCK_IN");
        transaction.setQuantity(event.getQuantity());
        transaction.setUnit(event.getUnit());
        transaction.setUnitCost(event.getUnitCost());
        transaction.setTransactionDate(event.getTransactionDate());
        transaction.setReference(event.getReference());
        transaction.setNotes(event.getNotes());
        transaction.setCreatedBy(event.getCreatedBy());
        transaction.setStockBefore(event.getStockBefore());
        transaction.setStockAfter(event.getStockAfter());
        
        stockTransactionRepository.save(transaction);
    }
    
    @EventHandler
    public void on(StockOutEvent event) {
        // Update ingredient stock
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.setCurrentStock(event.getStockAfter());
            ingredientRepository.save(ingredient);
        }
        
        // Create stock transaction record
        StockTransaction transaction = new StockTransaction();
        transaction.setTransactionId(event.getTransactionId());
        transaction.setIngredientId(event.getIngredientId());
        transaction.setTransactionType("STOCK_OUT");
        transaction.setQuantity(event.getQuantity());
        transaction.setUnit(event.getUnit());
        transaction.setTransactionDate(event.getTransactionDate());
        transaction.setReference(event.getReference());
        transaction.setReason(event.getReason());
        transaction.setNotes(event.getNotes());
        transaction.setCreatedBy(event.getCreatedBy());
        transaction.setStockBefore(event.getStockBefore());
        transaction.setStockAfter(event.getStockAfter());
        
        stockTransactionRepository.save(transaction);
    }
    
    @EventHandler
    public void on(StockAdjustedEvent event) {
        // Update ingredient stock
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.setCurrentStock(event.getStockAfter());
            ingredientRepository.save(ingredient);
        }
        
        // Create stock transaction record
        StockTransaction transaction = new StockTransaction();
        transaction.setTransactionId(event.getTransactionId());
        transaction.setIngredientId(event.getIngredientId());
        transaction.setTransactionType("ADJUSTMENT");
        transaction.setQuantity(Math.abs(event.getAdjustmentQuantity()));
        transaction.setUnit(event.getUnit());
        transaction.setTransactionDate(event.getTransactionDate());
        transaction.setReason(event.getReason());
        transaction.setNotes(event.getNotes());
        transaction.setCreatedBy(event.getCreatedBy());
        transaction.setStockBefore(event.getStockBefore());
        transaction.setStockAfter(event.getStockAfter());
        
        stockTransactionRepository.save(transaction);
    }
    
    @EventHandler
    public void on(StockTakenEvent event) {
        // Update ingredient stock
        Ingredient ingredient = ingredientRepository.findById(event.getIngredientId()).orElse(null);
        if (ingredient != null) {
            ingredient.setCurrentStock(event.getStockAfter());
            ingredientRepository.save(ingredient);
        }
        
        // Create stock transaction record
        StockTransaction transaction = new StockTransaction();
        transaction.setTransactionId(event.getTransactionId());
        transaction.setIngredientId(event.getIngredientId());
        transaction.setTransactionType("STOCK_TAKE");
        transaction.setQuantity(event.getActualQuantity());
        transaction.setUnit(event.getUnit());
        transaction.setTransactionDate(event.getTransactionDate());
        transaction.setNotes(event.getNotes() + " (Variance: " + event.getVariance() + ")");
        transaction.setCreatedBy(event.getCreatedBy());
        transaction.setStockBefore(event.getStockBefore());
        transaction.setStockAfter(event.getStockAfter());
        
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
    }
}
