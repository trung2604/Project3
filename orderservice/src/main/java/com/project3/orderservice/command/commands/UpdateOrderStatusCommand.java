package com.project3.orderservice.command.commands;

import com.project3.orderservice.command.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderStatusCommand {
    @TargetAggregateIdentifier
    private String orderId;
    
    private OrderStatus newStatus;
    private String updatedBy;
    private String notes;
}

