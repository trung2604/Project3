package com.project3.inventoryservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingredients")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ingredient {
    
    @Id
    private String ingredientId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private String unit; // kg, liter, piece, etc.
    
    @Column(nullable = false)
    private Double currentStock;
    
    @Column(nullable = false)
    private Double minStockLevel; // Ngưỡng cảnh báo
    
    @Column(nullable = false)
    private Double maxStockLevel; // Mức tồn tối đa
    
    private LocalDate expiryDate; // Hạn sử dụng
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Supplier information
    private String supplierName;
    private String supplierContact;
    
    // Cost information
    private Double unitCost;
    private String currency = "VND";
    
    // Category
    private String category; // Raw Material, Spice, Beverage, etc.

    // Image
    private String imageUrl;
    private String imagePublicId;
    
    /**
     * Business logic: Updates current stock
     * This method encapsulates the business rule for stock updates
     */
    public void updateStock(Double newStock) {
        if (newStock == null || newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.currentStock = newStock;
    }
    
    /**
     * Business logic: Checks if ingredient has low stock
     */
    public boolean isLowStock() {
        return this.currentStock <= this.minStockLevel;
    }
    
    /**
     * Business logic: Checks if ingredient is out of stock
     */
    public boolean isOutOfStock() {
        return this.currentStock <= 0;
    }
    
    /**
     * Business logic: Checks if ingredient is expiring within warning days
     */
    public boolean isExpiring(java.time.LocalDate warningDate) {
        if (this.expiryDate == null) {
            return false;
        }
        return !this.expiryDate.isAfter(warningDate);
    }
    
    /**
     * Business logic: Checks if ingredient has expired
     */
    public boolean isExpired() {
        if (this.expiryDate == null) {
            return false;
        }
        return this.expiryDate.isBefore(java.time.LocalDate.now());
    }
    
    /**
     * Business logic: Checks if ingredient needs restocking
     */
    public boolean needsRestocking() {
        return this.currentStock < this.minStockLevel;
    }
    
    /**
     * Business logic: Toggles active status
     */
    public void toggleActive() {
        this.active = !this.active;
    }
    
    /**
     * Business logic: Deactivates ingredient (soft delete)
     */
    public void deactivate() {
        this.active = false;
    }
}
