package com.project3.paymentservice.command.commands;

import com.project3.paymentservice.command.enums.PaymentMethod;
import com.project3.paymentservice.command.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentCommand {
    
    @TargetAggregateIdentifier
    private String paymentId;
    
    private String orderId;
    private String customerId;
    private Double amount;
    private PaymentMethod paymentMethod;
    private PaymentType paymentType;
    private String ipAddress;
    private String userAgent;
}
