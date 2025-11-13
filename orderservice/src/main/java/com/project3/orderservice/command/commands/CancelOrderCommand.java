package com.project3.orderservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelOrderCommand {
    @TargetAggregateIdentifier
    private String orderId;
    
    private String cancellationReason;
    private String cancelledBy;
    
    private Boolean allowCancellation;
}

