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
public class UpdateOrderPaymentStatusCommand {
    @TargetAggregateIdentifier
    private String orderId;
    private String paymentStatus;
}
