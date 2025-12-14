package com.project3.loyaltyservice.command.entity;

import com.project3.loyaltyservice.command.enums.PointsTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_transactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointsTransaction {
    @Id
    private String transactionId;
    
    @Column(nullable = false)
    private String accountId;
    
    @Column(nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointsTransactionType type;
    
    @Column(nullable = false)
    private Long points;
    
    private Long pointsBefore;
    
    private Long pointsAfter;
    
    private String orderId; // For EARNED type
    
    private String voucherId; // For REDEEMED type
    
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

