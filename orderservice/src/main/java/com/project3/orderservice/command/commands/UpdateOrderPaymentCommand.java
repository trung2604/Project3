package com.project3.orderservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderPaymentCommand {
    @TargetAggregateIdentifier
    private String orderId;
    private String paymentId;
    private String paymentStatus; // Add status
    private com.project3.orderservice.command.enums.OrderStatus status; // Optional: update status if needed
}
