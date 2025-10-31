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
}
