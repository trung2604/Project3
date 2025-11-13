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
}
