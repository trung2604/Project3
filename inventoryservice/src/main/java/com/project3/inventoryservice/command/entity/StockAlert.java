package com.project3.inventoryservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_alerts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAlert {
    
    @Id
    private String alertId;
    
    @Column(nullable = false)
    private String ingredientId;
    
    @Column(nullable = false)
    private String alertType; // LOW_STOCK, EXPIRED, EXPIRING_SOON
    
    @Column(nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(nullable = false)
    private String message;
    
    @Column(nullable = false)
    private LocalDateTime alertDate;
    
    private LocalDateTime acknowledgedAt;
    
    private String acknowledgedBy;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    // Additional data for different alert types
    private Double currentStock;
    private Double minStockLevel;
    private String expiryDate; // For expiry alerts
}
