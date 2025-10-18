package com.project3.inventoryservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateIngredientCommand {
    @TargetAggregateIdentifier
    private String ingredientId;
    private String name;
    private String description;
    private String unit;
    private Double initialStock;
    private Double minStockLevel;
    private Double maxStockLevel;
    private LocalDate expiryDate;
    private String supplierName;
    private String supplierContact;
    private Double unitCost;
    private String currency;
    private String category;
}
