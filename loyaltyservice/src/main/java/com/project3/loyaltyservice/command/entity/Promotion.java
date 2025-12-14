package com.project3.loyaltyservice.command.entity;

import com.project3.loyaltyservice.command.enums.PromotionStatus;
import com.project3.loyaltyservice.command.enums.PromotionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "promotions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Promotion {
    @Id
    private String promotionId;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status = PromotionStatus.ACTIVE;
    
    // For COMBO promotion
    @ElementCollection
    @CollectionTable(name = "promotion_menu_items", joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "menu_item_id")
    private List<String> menuItemIds;
    
    // For HAPPY_HOUR promotion
    private String dayOfWeek; // MONDAY, TUESDAY, etc.
    private String startTime; // HH:mm format
    private String endTime;   // HH:mm format
    
    // Discount configuration
    private Double discountPercentage;
    private Double discountAmount;
    private Double maxDiscountAmount;
    private Double minOrderAmount;
    
    // Points multiplier
    private Double pointsMultiplier; // e.g., 1.5x points
    
    // Validity period
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    
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
}

