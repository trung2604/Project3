package com.project3.paymentservice.command.events;

import com.project3.paymentservice.command.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentProcessedEvent {
    private String paymentId;
    private String orderId;
    private String customerId;
    private Double amount;
    private PaymentStatus status;
    private String transactionReference;
    private String gatewayOrderId;
    private String gatewayTransactionId;
    private String gatewayResponse;
    private LocalDateTime processedAt;
    private String failureReason;
}
