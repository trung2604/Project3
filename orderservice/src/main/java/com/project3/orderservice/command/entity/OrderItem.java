package com.project3.orderservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    @Id
    private String orderItemId;
    
    @Column(nullable = false)
    private String orderId;
    
    @Column(nullable = false)
    private String menuItemId;
    
    private String name;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Double unitPrice;
    
    @Column(nullable = false)
    private Double subtotal;
    
    private String notes;
}

