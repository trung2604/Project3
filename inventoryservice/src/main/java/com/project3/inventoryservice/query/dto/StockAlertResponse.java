package com.project3.inventoryservice.query.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAlertResponse {
    private String alertId;
    private String ingredientId;
    private String ingredientName;
    private String alertType;
    private String severity;
    private String message;
    private LocalDateTime alertDate;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private Boolean isActive;
    private Double currentStock;
    private Double minStockLevel;
    private String expiryDate;
}
