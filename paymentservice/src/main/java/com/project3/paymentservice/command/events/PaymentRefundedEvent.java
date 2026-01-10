package com.project3.paymentservice.command.events;

import com.project3.paymentservice.command.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRefundedEvent {
    private String paymentId;
    private String orderId;
    private String customerId;
    private Double refundAmount;
    private PaymentStatus newStatus;
    private String reason;
    private String requestedBy;
    private LocalDateTime refundedAt;
    private String gatewayResponse;
}
