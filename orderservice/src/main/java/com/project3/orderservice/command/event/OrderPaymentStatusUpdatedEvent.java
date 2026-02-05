package com.project3.orderservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPaymentStatusUpdatedEvent {
    private String orderId;
    private String paymentStatus;
}
