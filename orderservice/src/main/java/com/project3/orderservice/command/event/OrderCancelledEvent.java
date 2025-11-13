package com.project3.orderservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCancelledEvent {
    private String orderId;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
}

