package com.project3.inventoryservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockTakenEvent {
    private String ingredientId;
    private String transactionId;
    private Double actualQuantity;
    private Double systemQuantity;
    private Double variance;
    private String unit;
    private LocalDateTime transactionDate;
    private String notes;
    private String createdBy;
    private Double stockBefore;
    private Double stockAfter;
}
