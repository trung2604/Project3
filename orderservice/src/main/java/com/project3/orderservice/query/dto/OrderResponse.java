package com.project3.orderservice.query.dto;

import com.project3.orderservice.command.dto.OrderItemDTO;
import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private String orderId;
    private String paymentId;
    private String paymentStatus;
    private String paymentMethod;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private OrderType orderType;
    private OrderStatus orderStatus;
    
    private List<OrderItemDTO> orderItems;
    
    private Double subtotal;
    private Double discountAmount;
    private Double discountPercentage;
    private Double vatAmount;
    private Double vatPercentage;
    private Double totalAmount;
    
    private LocalDateTime orderDate;
    private LocalDateTime cookingStartTime;
    private LocalDateTime readyTime;
    private LocalDateTime completedTime;
    private LocalDateTime cancelledTime;
    
    private String deliveryAddress;
    private String tableNumber;
    private String notes;
    private String createdBy;
    private String cancellationReason;
}

