package com.project3.inventoryservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockOutCommand {
    @TargetAggregateIdentifier
    private String ingredientId;
    private String transactionId;
    private Double quantity;
    private String unit;
    private LocalDateTime transactionDate;
    private String reference;
    private String reason;
    private String notes;
    private String createdBy;
}
