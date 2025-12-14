package com.project3.loyaltyservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoyaltyAccount {
    @Id
    private String accountId;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    @Column(nullable = false)
    private Long currentPoints = 0L;
    
    @Column(nullable = false)
    private Long totalPointsEarned = 0L;
    
    @Column(nullable = false)
    private Long totalPointsRedeemed = 0L;
    
    private String tier; // BRONZE, SILVER, GOLD, PLATINUM
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business logic: Earns points and updates account statistics
     * This method encapsulates the business rule for earning points
     */
    public void earnPoints(Long points) {
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        this.currentPoints += points;
        this.totalPointsEarned += points;
    }
    
    /**
     * Business logic: Redeems points and updates account statistics
     * This method encapsulates the business rule for redeeming points
     */
    public void redeemPoints(Long points) {
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        if (this.currentPoints < points) {
            throw new IllegalStateException(
                String.format("Insufficient points. Required: %d, Available: %d", points, this.currentPoints)
            );
        }
        this.currentPoints -= points;
        this.totalPointsRedeemed += points;
    }
    
    /**
     * Business logic: Checks if account has enough points
     */
    public boolean hasEnoughPoints(Long required) {
        return this.currentPoints >= required;
    }
    
    /**
     * Business logic: Updates points from event (used in event handlers)
     * This method handles both earning and redeeming based on points sign
     */
    public void updatePointsFromEvent(Long points, Long pointsAfter) {
        this.currentPoints = pointsAfter;
        if (points > 0) {
            this.totalPointsEarned += points;
        } else {
            this.totalPointsRedeemed += Math.abs(points);
        }
    }
}

