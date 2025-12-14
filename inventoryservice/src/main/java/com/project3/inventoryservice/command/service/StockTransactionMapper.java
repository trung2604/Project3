package com.project3.inventoryservice.command.service;

import com.project3.inventoryservice.command.entity.StockTransaction;
import com.project3.inventoryservice.command.event.*;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between StockTransaction entities and Events
 * Encapsulates mapping logic for better cohesion
 */
@Component
public class StockTransactionMapper {
    
    /**
     * Creates StockTransaction from StockInEvent
     */
    public StockTransaction fromStockInEvent(StockInEvent event) {
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
        return transaction;
    }
    
    /**
     * Creates StockTransaction from StockOutEvent
     */
    public StockTransaction fromStockOutEvent(StockOutEvent event) {
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
        return transaction;
    }
    
    /**
     * Creates StockTransaction from StockAdjustedEvent
     */
    public StockTransaction fromStockAdjustedEvent(StockAdjustedEvent event) {
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
        return transaction;
    }
    
    /**
     * Creates StockTransaction from StockTakenEvent
     */
    public StockTransaction fromStockTakenEvent(StockTakenEvent event) {
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
        return transaction;
    }
    
    /**
     * Creates initial stock transaction for new ingredient
     */
    public StockTransaction createInitialStockTransaction(String ingredientId, Double initialStock, String unit, java.time.LocalDateTime createdAt) {
        StockTransaction transaction = new StockTransaction();
        transaction.setTransactionId("init-" + ingredientId);
        transaction.setIngredientId(ingredientId);
        transaction.setTransactionType("INITIAL_STOCK");
        transaction.setQuantity(initialStock);
        transaction.setUnit(unit);
        transaction.setTransactionDate(createdAt);
        transaction.setReference("Initial Stock");
        transaction.setCreatedBy("SYSTEM");
        transaction.setStockBefore(0.0);
        transaction.setStockAfter(initialStock);
        return transaction;
    }
}

