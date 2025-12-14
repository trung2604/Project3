package com.project3.loyaltyservice.command.entity;

import com.project3.loyaltyservice.command.enums.VoucherUsageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_usages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherUsage {
    @Id
    private String usageId;
    
    @Column(nullable = false)
    private String voucherId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String accountId;
    
    private String orderId; // Order where voucher was used
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherUsageStatus status = VoucherUsageStatus.USED;
    
    private Long pointsRedeemed;
    
    private Double discountApplied;
    
    @Column(nullable = false)
    private LocalDateTime usedAt;
    
    private LocalDateTime cancelledAt;
    
    @PrePersist
    protected void onCreate() {
        usedAt = LocalDateTime.now();
    }
}

