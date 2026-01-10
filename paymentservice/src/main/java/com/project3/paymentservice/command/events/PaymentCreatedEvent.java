package com.project3.paymentservice.command.events;

import com.project3.paymentservice.command.enums.PaymentMethod;
import com.project3.paymentservice.command.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCreatedEvent {
    private String paymentId;
    private String orderId;
    private String customerId;
    private Double amount;
    private PaymentMethod paymentMethod;
    private PaymentType paymentType;
    private LocalDateTime createdAt;
    private String ipAddress;
    private String userAgent;
}
