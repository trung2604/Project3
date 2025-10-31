package com.project3.inventoryservice.command.aggregate;

import com.project3.inventoryservice.command.commands.*;
import com.project3.inventoryservice.command.event.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Aggregate
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredientAggregate {
    
    @AggregateIdentifier
    private String ingredientId;
    
    private String name;
    private String description;
    private String unit;
    private Double currentStock;
    private Double minStockLevel;
    private Double maxStockLevel;
    private LocalDate expiryDate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String supplierName;
    private String supplierContact;
    private Double unitCost;
    private String currency;
    private String category;
    private String imageUrl;
    private String imagePublicId;
    
    @CommandHandler
    public IngredientAggregate(CreateIngredientCommand command) {
        IngredientCreatedEvent event = new IngredientCreatedEvent();
        BeanUtils.copyProperties(command, event);
        event.setCreatedAt(LocalDateTime.now());
        AggregateLifecycle.apply(event);
    }
    
    @CommandHandler
    public void handle(UpdateIngredientCommand command) {
        IngredientUpdatedEvent event = new IngredientUpdatedEvent();
        BeanUtils.copyProperties(command, event);
        event.setUpdatedAt(LocalDateTime.now());
        AggregateLifecycle.apply(event);
    }
    
    @CommandHandler
    public void handle(DeleteIngredientCommand command) {
        IngredientDeletedEvent event = new IngredientDeletedEvent();
        event.setIngredientId(command.getIngredientId());
        AggregateLifecycle.apply(event);
    }
    
    @CommandHandler
    public void handle(ToggleIngredientActiveCommand command) {
        IngredientToggledEvent event = new IngredientToggledEvent();
        event.setIngredientId(command.getIngredientId());
        event.setActive(!this.active);
        AggregateLifecycle.apply(event);
    }
    
    @CommandHandler
    public void handle(StockInCommand command) {
        // Validate stock level
        if (command.getQuantity() <= 0) {
            throw new IllegalArgumentException("Stock in quantity must be positive");
        }
        
        Double newStock = this.currentStock + command.getQuantity();
        
        // Check if exceeding max stock level
        if (newStock > this.maxStockLevel) {
            throw new IllegalArgumentException("Stock level would exceed maximum allowed: " + this.maxStockLevel);
        }
        
        StockInEvent event = new StockInEvent();
        BeanUtils.copyProperties(command, event);
        event.setStockBefore(this.currentStock);
        event.setStockAfter(newStock);
        AggregateLifecycle.apply(event);
    }
    
    @CommandHandler
    public void handle(StockOutCommand command) {
        // Validate stock level
        if (command.getQuantity() <= 0) {
            throw new IllegalArgumentException("Stock out quantity must be positive");
        }
        
        if (command.getQuantity() > this.currentStock) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + this.currentStock + ", Requested: " + command.getQuantity());
        }
        
        Double newStock = this.currentStock - command.getQuantity();
        
        StockOutEvent event = new StockOutEvent();
        BeanUtils.copyProperties(command, event);
        event.setStockBefore(this.currentStock);
        event.setStockAfter(newStock);
        AggregateLifecycle.apply(event);
        
        // Check for low stock alert
        if (newStock <= this.minStockLevel) {
            LowStockAlertEvent alertEvent = new LowStockAlertEvent();
            alertEvent.setIngredientId(this.ingredientId);
            alertEvent.setIngredientName(this.name);
            alertEvent.setCurrentStock(newStock);
            alertEvent.setMinStockLevel(this.minStockLevel);
            alertEvent.setSeverity(newStock <= 0 ? "CRITICAL" : "HIGH");
            alertEvent.setMessage("Low stock alert for " + this.name + ". Current: " + newStock + ", Min: " + this.minStockLevel);
            alertEvent.setAlertDate(LocalDateTime.now());
            AggregateLifecycle.apply(alertEvent);
        }
    }
    
    @CommandHandler
    public void handle(AdjustStockCommand command) {
        Double newStock = this.currentStock + command.getAdjustmentQuantity();
        
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock adjustment would result in negative stock");
        }
        
        StockAdjustedEvent event = new StockAdjustedEvent();
        BeanUtils.copyProperties(command, event);
        event.setStockBefore(this.currentStock);
        event.setStockAfter(newStock);
        AggregateLifecycle.apply(event);
        
        // Check for low stock alert
        if (newStock <= this.minStockLevel) {
            LowStockAlertEvent alertEvent = new LowStockAlertEvent();
            alertEvent.setIngredientId(this.ingredientId);
            alertEvent.setIngredientName(this.name);
            alertEvent.setCurrentStock(newStock);
            alertEvent.setMinStockLevel(this.minStockLevel);
            alertEvent.setSeverity(newStock <= 0 ? "CRITICAL" : "HIGH");
            alertEvent.setMessage("Low stock alert for " + this.name + ". Current: " + newStock + ", Min: " + this.minStockLevel);
            alertEvent.setAlertDate(LocalDateTime.now());
            AggregateLifecycle.apply(alertEvent);
        }
    }
    
    @CommandHandler
    public void handle(StockTakeCommand command) {
        Double variance = command.getActualQuantity() - this.currentStock;
        
        StockTakenEvent event = new StockTakenEvent();
        BeanUtils.copyProperties(command, event);
        event.setSystemQuantity(this.currentStock);
        event.setVariance(variance);
        event.setStockBefore(this.currentStock);
        event.setStockAfter(command.getActualQuantity());
        AggregateLifecycle.apply(event);
        
        // Check for low stock alert after stock take
        if (command.getActualQuantity() <= this.minStockLevel) {
            LowStockAlertEvent alertEvent = new LowStockAlertEvent();
            alertEvent.setIngredientId(this.ingredientId);
            alertEvent.setIngredientName(this.name);
            alertEvent.setCurrentStock(command.getActualQuantity());
            alertEvent.setMinStockLevel(this.minStockLevel);
            alertEvent.setSeverity(command.getActualQuantity() <= 0 ? "CRITICAL" : "HIGH");
            alertEvent.setMessage("Low stock alert for " + this.name + ". Current: " + command.getActualQuantity() + ", Min: " + this.minStockLevel);
            alertEvent.setAlertDate(LocalDateTime.now());
            AggregateLifecycle.apply(alertEvent);
        }
    }
    
    // Event Sourcing Handlers
    @EventSourcingHandler
    public void on(IngredientCreatedEvent event) {
        this.ingredientId = event.getIngredientId();
        this.name = event.getName();
        this.description = event.getDescription();
        this.unit = event.getUnit();
        this.currentStock = event.getInitialStock();
        this.minStockLevel = event.getMinStockLevel();
        this.maxStockLevel = event.getMaxStockLevel();
        this.expiryDate = event.getExpiryDate();
        this.active = true;
        this.createdAt = event.getCreatedAt();
        this.supplierName = event.getSupplierName();
        this.supplierContact = event.getSupplierContact();
        this.unitCost = event.getUnitCost();
        this.currency = event.getCurrency();
        this.category = event.getCategory();
        this.imageUrl = event.getImageUrl();
        this.imagePublicId = event.getImagePublicId();
    }
    
    @EventSourcingHandler
    public void on(IngredientUpdatedEvent event) {
        this.name = event.getName();
        this.description = event.getDescription();
        this.unit = event.getUnit();
        this.minStockLevel = event.getMinStockLevel();
        this.maxStockLevel = event.getMaxStockLevel();
        this.expiryDate = event.getExpiryDate();
        this.updatedAt = event.getUpdatedAt();
        this.supplierName = event.getSupplierName();
        this.supplierContact = event.getSupplierContact();
        this.unitCost = event.getUnitCost();
        this.currency = event.getCurrency();
        this.category = event.getCategory();
        this.imageUrl = event.getImageUrl();
        this.imagePublicId = event.getImagePublicId();
    }
    
    @EventSourcingHandler
    public void on(IngredientDeletedEvent event) {
        this.active = false;
    }
    
    @EventSourcingHandler
    public void on(IngredientToggledEvent event) {
        this.active = event.getActive();
    }
    
    @EventSourcingHandler
    public void on(StockInEvent event) {
        this.currentStock = event.getStockAfter();
    }
    
    @EventSourcingHandler
    public void on(StockOutEvent event) {
        this.currentStock = event.getStockAfter();
    }
    
    @EventSourcingHandler
    public void on(StockAdjustedEvent event) {
        this.currentStock = event.getStockAfter();
    }
    
    @EventSourcingHandler
    public void on(StockTakenEvent event) {
        this.currentStock = event.getStockAfter();
    }
}
