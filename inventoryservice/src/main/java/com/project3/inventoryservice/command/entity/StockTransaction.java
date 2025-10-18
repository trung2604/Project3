package com.project3.inventoryservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockTransaction {
    
    @Id
    private String transactionId;
    
    @Column(nullable = false)
    private String ingredientId;
    
    @Column(nullable = false)
    private String transactionType; // STOCK_IN, STOCK_OUT, ADJUSTMENT, STOCK_TAKE
    
    @Column(nullable = false)
    private Double quantity;
    
    @Column(nullable = false)
    private String unit;
    
    private Double unitCost;
    
    @Column(nullable = false)
    private LocalDateTime transactionDate;
    
    private String reference; // Reference number, invoice number, etc.
    
    private String reason; // Reason for adjustment, stock take, etc.
    
    private String notes;
    
    @Column(nullable = false)
    private String createdBy; // User who created the transaction
    
    // Stock levels before and after transaction
    private Double stockBefore;
    private Double stockAfter;
}
