package com.project3.inventoryservice.query.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockTransactionResponse {
    private String transactionId;
    private String ingredientId;
    private String ingredientName;
    private String transactionType;
    private Double quantity;
    private String unit;
    private Double unitCost;
    private LocalDateTime transactionDate;
    private String reference;
    private String reason;
    private String notes;
    private String createdBy;
    private Double stockBefore;
    private Double stockAfter;
}
