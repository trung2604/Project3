package com.project3.inventoryservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockOutEvent {
    private String ingredientId;
    private String transactionId;
    private Double quantity;
    private String unit;
    private LocalDateTime transactionDate;
    private String reference;
    private String reason;
    private String notes;
    private String createdBy;
    private Double stockBefore;
    private Double stockAfter;
}
