package com.project3.inventoryservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockInCommand {
    @TargetAggregateIdentifier
    private String ingredientId;
    private String transactionId;
    private Double quantity;
    private String unit;
    private Double unitCost;
    private LocalDateTime transactionDate;
    private String reference;
    private String notes;
    private String createdBy;
}
