package com.project3.menuservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for attaching ingredients with quantities to menu items
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachIngredientsWithQuantityDTO {
    private String ingredientId;
    private Double quantity;  // Quantity needed per serving (e.g., 0.2 kg)
    private String unit;       // Unit of measurement (e.g., "kg", "liter")
    private String notes;      // Optional notes
}

