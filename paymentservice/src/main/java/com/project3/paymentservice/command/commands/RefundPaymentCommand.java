package com.project3.paymentservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundPaymentCommand {
    
    @TargetAggregateIdentifier
    private String paymentId;
    
    private Double refundAmount;
    private String reason;
    private String requestedBy;
}
