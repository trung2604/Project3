package com.project3.paymentservice.command.dto;

import com.project3.paymentservice.command.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {
    private String orderId;
    private String customerId;
    private Double amount;
    private PaymentMethod paymentMethod;
}
