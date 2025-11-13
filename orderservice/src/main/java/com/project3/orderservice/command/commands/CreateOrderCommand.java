package com.project3.orderservice.command.commands;

import com.project3.orderservice.command.dto.OrderItemDTO;
import com.project3.orderservice.command.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderCommand {
    @TargetAggregateIdentifier
    private String orderId;
    
    private String customerId;
    private String customerName;
    private String customerPhone;
    
    @TargetAggregateIdentifier
    private OrderType orderType;
    
    private List<OrderItemDTO> orderItems;
    
    private Double discountAmount;
    private Double discountPercentage;
    
    private Double vatPercentage;
    
    private String deliveryAddress;
    private String tableNumber;
    
    private String notes;
    private String createdBy;
}
