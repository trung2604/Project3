package com.project3.paymentservice.command.service.payment;

import com.project3.paymentservice.command.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String paymentId;
    private String orderId;
    private String customerId;
    private Double amount;
    private PaymentMethod paymentMethod;
    private String returnUrl;
    private String cancelUrl;
}
