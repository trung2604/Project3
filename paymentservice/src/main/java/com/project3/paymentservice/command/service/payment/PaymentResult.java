package com.project3.paymentservice.command.service.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResult {
    private boolean success;
    private String gatewayOrderId;
    private String gatewayTransactionId;
    private String redirectUrl;
    private String qrCodeData;
    private String message;
    private String errorMessage;
}
