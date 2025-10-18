package com.project3.inventoryservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockTakeCommand {
    @TargetAggregateIdentifier
    private String ingredientId;
    private String transactionId;
    private Double actualQuantity; // Actual physical count
    private String unit;
    private LocalDateTime transactionDate;
    private String notes;
    private String createdBy;
}
