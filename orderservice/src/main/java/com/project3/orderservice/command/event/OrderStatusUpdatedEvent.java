package com.project3.orderservice.command.event;

import com.project3.orderservice.command.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusUpdatedEvent {
    private String orderId;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String notes;
}

