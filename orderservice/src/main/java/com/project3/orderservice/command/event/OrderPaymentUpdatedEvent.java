package com.project3.orderservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPaymentUpdatedEvent {
    private String orderId;
    private String paymentId;
    private String paymentStatus;
}
