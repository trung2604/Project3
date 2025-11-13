package com.project3.orderservice.command.event;

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
public class CreateOrderEvent {
    private String orderId;
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
    private String deliveryAddress;
    private String tableNumber;
    private String notes;
    private String createdBy;
}
