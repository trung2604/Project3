package com.project3.inventoryservice.query.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredientResponse {
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
}
