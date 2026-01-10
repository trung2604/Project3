package com.project3.paymentservice.command.service.payment;

import com.project3.paymentservice.command.enums.PaymentMethod;

public interface PaymentProcessor {
    PaymentResult processPayment(PaymentRequest request);
    PaymentResult processRefund(String paymentId, Double amount, String reason);
    boolean supports(PaymentMethod method);
}
