package com.project3.orderservice.command.entity;

import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    @Id
    private String orderId;

    private String customerId;
    
    private String paymentId;
    
    private String paymentStatus;
    
    private String paymentMethod;
    
    private String customerName;
    
    private String customerPhone;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private Double subtotal;
    
    private Double discountAmount;
    
    private Double discountPercentage;
    
    @Column(nullable = false)
    private Double vatAmount;
    
    @Column(nullable = false)
    private Double vatPercentage;
    
    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private LocalDateTime orderDate;
    
    private LocalDateTime cookingStartTime;
    
    private LocalDateTime readyTime;
    
    private LocalDateTime completedTime;
    
    private LocalDateTime cancelledTime;
    
    private String cancellationReason;
    
    private String deliveryAddress;
    
    private String tableNumber;
    
    private String notes;
    
    private String createdBy;
    
    @OneToMany(mappedBy = "orderId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();
    
    /**
     * Business logic: Updates order status and sets appropriate timestamps
     * This method encapsulates the business rule that different statuses require different timestamp updates
     */
    public void updateStatus(OrderStatus newStatus, LocalDateTime updatedAt) {
        this.orderStatus = newStatus;
        
        switch (newStatus) {
            case COOKING:
                if (this.cookingStartTime == null) {
                    this.cookingStartTime = updatedAt;
                }
                break;
            case READY:
                this.readyTime = updatedAt;
                break;
            case COMPLETED:
                this.completedTime = updatedAt;
                break;
            default:
                // Other statuses don't require timestamp updates
                break;
        }
    }
    
    /**
     * Business logic: Checks if order should trigger delivery request
     * This method encapsulates the business rule for delivery triggering
     */
    public boolean shouldTriggerDelivery() {
        return this.orderStatus == OrderStatus.READY 
            && this.orderType == OrderType.DELIVERY;
    }
    
    /**
     * Business logic: Cancels the order
     * This method encapsulates the cancellation business logic
     */
    public void cancel(String reason, LocalDateTime cancelledAt) {
        this.orderStatus = OrderStatus.CANCELLED;
        this.cancelledTime = cancelledAt;
        this.cancellationReason = reason;
    }
    
    /**
     * Business logic: Checks if order can be cancelled
     */
    public boolean canBeCancelled() {
        return this.orderStatus != OrderStatus.COMPLETED 
            && this.orderStatus != OrderStatus.CANCELLED;
    }
}
