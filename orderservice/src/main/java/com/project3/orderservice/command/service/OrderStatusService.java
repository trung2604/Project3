package com.project3.orderservice.command.service;

import com.project3.orderservice.command.constants.OrderConstants;
import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.enums.OrderType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for handling order status updates and related business logic
 * Encapsulates status-related logic for better cohesion
 * 
 * Note: Most business logic has been moved to Order entity.
 * This service now only contains logic that requires external dependencies.
 */
@Service
public class OrderStatusService {
    
    /**
     * Updates order status using entity method
     * This is a convenience method that delegates to Order entity
     */
    public void updateOrderStatus(Order order, OrderStatus newStatus, LocalDateTime updatedAt) {
        order.updateStatus(newStatus, updatedAt);
    }
    
    /**
     * Checks if order should trigger delivery request using entity method
     * This is a convenience method that delegates to Order entity
     */
    public boolean shouldTriggerDelivery(Order order) {
        return order.shouldTriggerDelivery();
    }
}

