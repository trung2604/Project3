package com.project3.loyaltyservice.command.entity;

import com.project3.loyaltyservice.command.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Voucher {
    @Id
    private String voucherId;
    
    @Column(nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private Long pointsRequired;
    
    @Column(nullable = false)
    private Double discountAmount;
    
    private Double discountPercentage;
    
    private Double maxDiscountAmount;
    
    private Double minOrderAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherStatus status = VoucherStatus.ACTIVE;
    
    @Column(nullable = false)
    private Integer totalQuantity;
    
    @Column(nullable = false)
    private Integer remainingQuantity;
    
    private LocalDateTime validFrom;
    
    private LocalDateTime validTo;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (remainingQuantity == null) {
            remainingQuantity = totalQuantity;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business logic: Checks if voucher is active
     */
    public boolean isActive() {
        return this.status == VoucherStatus.ACTIVE;
    }
    
    /**
     * Business logic: Checks if voucher is available (active and has remaining quantity)
     */
    public boolean isAvailable() {
        return isActive() && this.remainingQuantity > 0;
    }
    
    /**
     * Business logic: Validates if voucher can be redeemed by an account
     * @throws IllegalStateException if validation fails
     */
    public void validateRedemption(com.project3.loyaltyservice.command.entity.LoyaltyAccount account) {
        if (!isActive()) {
            throw new IllegalStateException("Voucher is not active");
        }
        
        if (this.remainingQuantity <= 0) {
            throw new IllegalStateException("Voucher is out of stock");
        }
        
        if (account == null || !account.hasEnoughPoints(this.pointsRequired)) {
            throw new IllegalStateException(
                String.format("Insufficient points. Required: %d, Available: %d", 
                    this.pointsRequired, account != null ? account.getCurrentPoints() : 0)
            );
        }
    }
    
    /**
     * Business logic: Redeems the voucher (decreases remaining quantity)
     * This method should be called after validation
     */
    public void redeem() {
        if (this.remainingQuantity <= 0) {
            throw new IllegalStateException("Cannot redeem voucher: out of stock");
        }
        
        this.remainingQuantity--;
        
        if (this.remainingQuantity <= 0) {
            this.status = VoucherStatus.USED_UP;
        }
    }
    
    /**
     * Business logic: Checks if voucher is valid for a given date
     */
    public boolean isValidForDate(LocalDateTime date) {
        if (date == null) {
            date = LocalDateTime.now();
        }
        return (this.validFrom == null || date.isAfter(this.validFrom) || date.isEqual(this.validFrom))
            && (this.validTo == null || date.isBefore(this.validTo) || date.isEqual(this.validTo));
    }
}

