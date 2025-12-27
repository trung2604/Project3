package com.project3.orderservice.command.service;

import com.project3.commonservice.dto.DeliveryRequestEvent;
import com.project3.commonservice.dto.InventoryDeductionEvent;
import com.project3.commonservice.dto.PaymentRequestEvent;
import com.project3.commonservice.service.KafkaService;
import com.project3.orderservice.command.constants.OrderConstants;
import com.project3.orderservice.command.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing order-related Kafka events
 * Encapsulates all Kafka event publishing logic for better cohesion
 */
@Service
@Slf4j
public class OrderEventPublisher {
    
    @Autowired
    private KafkaService kafkaService;
    
    /**
     * Publishes payment request event when order is created
     */
    public void publishPaymentRequest(Order order) {
        try {
            PaymentRequestEvent paymentEvent = new PaymentRequestEvent();
            paymentEvent.setOrderId(order.getOrderId());
            paymentEvent.setAmount(order.getTotalAmount());
            paymentEvent.setCustomerId(order.getCustomerId());
            paymentEvent.setOrderType(order.getOrderType() != null ? order.getOrderType().name() : OrderConstants.TYPE_UNKNOWN);
            paymentEvent.setTimestamp(System.currentTimeMillis());
            
            kafkaService.sendMessage(OrderConstants.TOPIC_PAYMENT_REQUEST, paymentEvent);
            log.info("Payment request sent via Kafka for order {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send payment request for order {}: {}", order.getOrderId(), e.getMessage());
        }
    }
    
    /**
     * Publishes delivery request event when order is ready for delivery
     */
    public void publishDeliveryRequest(Order order) {
        try {
            DeliveryRequestEvent deliveryEvent = new DeliveryRequestEvent();
            deliveryEvent.setOrderId(order.getOrderId());
            deliveryEvent.setDeliveryAddress(order.getDeliveryAddress());
            deliveryEvent.setCustomerPhone(order.getCustomerPhone());
            deliveryEvent.setCustomerName(order.getCustomerName());
            deliveryEvent.setTotalAmount(order.getTotalAmount());
            deliveryEvent.setTimestamp(System.currentTimeMillis());
            
            kafkaService.sendMessage(OrderConstants.TOPIC_DELIVERY_REQUEST, deliveryEvent);
            log.info("Delivery request sent via Kafka for order {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send delivery request for order {}: {}", order.getOrderId(), e.getMessage());
        }
    }
    
    /**
     * Publishes order completed event for loyalty points
     */
    public void publishOrderCompleted(Order order, String completedAt) {
        if (order.getTotalAmount() == null || order.getCustomerId() == null) {
            log.warn("Cannot send order-completed event for order {}: totalAmount or customerId is null", order.getOrderId());
            return;
        }
        
        try {
            Map<String, Object> orderCompletedEvent = new HashMap<>();
            orderCompletedEvent.put("orderId", order.getOrderId());
            orderCompletedEvent.put("customerId", order.getCustomerId());
            orderCompletedEvent.put("totalAmount", order.getTotalAmount());
            orderCompletedEvent.put("orderType", order.getOrderType() != null ? order.getOrderType().name() : OrderConstants.TYPE_UNKNOWN);
            orderCompletedEvent.put("completedAt", completedAt != null ? completedAt : "");
            orderCompletedEvent.put("timestamp", System.currentTimeMillis());
            
            kafkaService.sendMessage(OrderConstants.TOPIC_ORDER_COMPLETED, orderCompletedEvent);
            log.info("Order completed event sent via Kafka for order {} - customer {} will earn points", 
                order.getOrderId(), order.getCustomerId());
        } catch (Exception e) {
            log.error("Failed to send order-completed event for order {}: {}", order.getOrderId(), e.getMessage());
        }
    }
    
    /**
     * Publishes inventory deduction event for an ingredient
     */
    public void publishInventoryDeduction(String orderId, String ingredientId, Double quantity, String reason) {
        try {
            InventoryDeductionEvent deductionEvent = new InventoryDeductionEvent();
            deductionEvent.setOrderId(orderId);
            deductionEvent.setIngredientId(ingredientId);
            // Use quantityDouble for precise quantities
            deductionEvent.setQuantityDouble(quantity);
            // Also set quantity for backward compatibility (rounded)
            deductionEvent.setQuantity(quantity != null ? quantity.intValue() : 0);
            deductionEvent.setUnit(OrderConstants.DEFAULT_UNIT);
            deductionEvent.setUnit(OrderConstants.DEFAULT_UNIT);
            deductionEvent.setReference(orderId);
            deductionEvent.setReason(reason != null ? reason : OrderConstants.DEFAULT_INVENTORY_REASON);
            deductionEvent.setTimestamp(System.currentTimeMillis());
            
            kafkaService.sendMessage(OrderConstants.TOPIC_INVENTORY_DEDUCTION_REQUEST, deductionEvent);
        } catch (Exception e) {
            log.error("Failed to send inventory deduction event for order {} ingredient {}: {}", 
                orderId, ingredientId, e.getMessage());
        }
    }
    
    /**
     * Overloaded method for backward compatibility with Integer quantity
     */
    public void publishInventoryDeduction(String orderId, String ingredientId, Integer quantity, String reason) {
        publishInventoryDeduction(orderId, ingredientId, quantity != null ? quantity.doubleValue() : 0.0, reason);
    }
    
    /**
     * Publishes order created event for notifications
     */
    public void publishOrderCreated(Order order) {
        try {
            Map<String, Object> orderCreatedEvent = new HashMap<>();
            orderCreatedEvent.put("orderId", order.getOrderId());
            orderCreatedEvent.put("customerId", order.getCustomerId());
            orderCreatedEvent.put("customerName", order.getCustomerName());
            orderCreatedEvent.put("totalAmount", order.getTotalAmount());
            orderCreatedEvent.put("orderType", order.getOrderType() != null ? order.getOrderType().name() : OrderConstants.TYPE_UNKNOWN);
            orderCreatedEvent.put("orderStatus", order.getOrderStatus() != null ? order.getOrderStatus().name() : "PENDING");
            orderCreatedEvent.put("createdAt", order.getOrderDate() != null ? order.getOrderDate().toString() : "");
            orderCreatedEvent.put("timestamp", System.currentTimeMillis());
            
            kafkaService.sendMessage(OrderConstants.TOPIC_ORDER_CREATED, orderCreatedEvent);
            log.info("Order created event sent via Kafka for order {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order-created event for order {}: {}", order.getOrderId(), e.getMessage());
        }
    }
    
    /**
     * Publishes order status updated event for notifications
     */
    public void publishOrderStatusUpdated(Order order, String previousStatus, String newStatus, String updatedBy) {
        try {
            Map<String, Object> statusUpdatedEvent = new HashMap<>();
            statusUpdatedEvent.put("orderId", order.getOrderId());
            statusUpdatedEvent.put("customerId", order.getCustomerId());
            statusUpdatedEvent.put("previousStatus", previousStatus);
            statusUpdatedEvent.put("newStatus", newStatus);
            statusUpdatedEvent.put("updatedBy", updatedBy);
            statusUpdatedEvent.put("orderType", order.getOrderType() != null ? order.getOrderType().name() : OrderConstants.TYPE_UNKNOWN);
            statusUpdatedEvent.put("totalAmount", order.getTotalAmount());
            statusUpdatedEvent.put("timestamp", System.currentTimeMillis());
            
            kafkaService.sendMessage(OrderConstants.TOPIC_ORDER_STATUS_UPDATED, statusUpdatedEvent);
            log.info("Order status updated event sent via Kafka for order {}: {} -> {}", 
                order.getOrderId(), previousStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to send order-status-updated event for order {}: {}", order.getOrderId(), e.getMessage());
        }
    }
    
    /**
     * Publishes order cancelled event for notifications
     */
    public void publishOrderCancelled(Order order, String cancellationReason) {
        try {
            Map<String, Object> orderCancelledEvent = new HashMap<>();
            orderCancelledEvent.put("orderId", order.getOrderId());
            orderCancelledEvent.put("customerId", order.getCustomerId());
            orderCancelledEvent.put("cancellationReason", cancellationReason);
            orderCancelledEvent.put("totalAmount", order.getTotalAmount());
            orderCancelledEvent.put("orderType", order.getOrderType() != null ? order.getOrderType().name() : OrderConstants.TYPE_UNKNOWN);
            orderCancelledEvent.put("timestamp", System.currentTimeMillis());
            
            kafkaService.sendMessage(OrderConstants.TOPIC_ORDER_CANCELLED, orderCancelledEvent);
            log.info("Order cancelled event sent via Kafka for order {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order-cancelled event for order {}: {}", order.getOrderId(), e.getMessage());
        }
    }
}

