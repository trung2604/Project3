package com.project3.menuservice.query.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for menu item ingredient with quantity information
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemIngredientDTO {
    private String ingredientId;
    private String ingredientName; // Optional: for display
    private Double quantity;       // Quantity needed per serving
    private String unit;           // Unit of measurement (kg, liter, etc.)
    private String notes;          // Optional notes
}

