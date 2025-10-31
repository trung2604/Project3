package com.project3.inventoryservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredientUpdatedEvent {
    private String ingredientId;
    private String name;
    private String description;
    private String unit;
    private Double minStockLevel;
    private Double maxStockLevel;
    private LocalDate expiryDate;
    private String supplierName;
    private String supplierContact;
    private Double unitCost;
    private String currency;
    private String category;
    private LocalDateTime updatedAt;
    private String imageUrl;
    private String imagePublicId;
}
