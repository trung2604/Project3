package com.project3.orderservice.command.event;

import com.project3.orderservice.command.commands.UpdateOrderStatusCommand;
import com.project3.orderservice.command.constants.OrderConstants;
import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.entity.OrderItem;
import com.project3.orderservice.command.entity.OrderItemRepository;
import com.project3.orderservice.command.entity.OrderRespository;
import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.service.IngredientExtractionService;
import com.project3.orderservice.command.service.OrderEventPublisher;
import com.project3.orderservice.command.service.OrderMapper;
import com.project3.orderservice.command.service.OrderStatusService;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OrderEventHandler {
    
    @Autowired
    private OrderRespository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private OrderEventPublisher eventPublisher;
    
    @Autowired
    private OrderStatusService statusService;
    
    @Autowired
    private IngredientExtractionService ingredientExtractionService;

    @Autowired
    private org.axonframework.commandhandling.gateway.CommandGateway commandGateway;

    @EventHandler
    public void on(CreateOrderEvent event) {
        log.info("Received CreateOrderEvent for order: {}", event.getOrderId());
        log.debug("Event payload: {}", event);

        if (orderRepository.existsById(event.getOrderId())) {
            log.warn("Order {} already exists in Read Model - Skipping save", event.getOrderId());
            return;
        }

        try {
            // Map event to entity
            Order order = orderMapper.toEntity(event);
            orderRepository.save(order);

            // Save order items
            List<OrderItem> orderItems = orderMapper.toOrderItemEntities(event.getOrderId(), event.getOrderItems());
            if (!orderItems.isEmpty()) {
                orderItemRepository.saveAll(orderItems);
            }

            log.info("Order {} created and saved to Read DB successfully", event.getOrderId());
            
            // Publish payment request event
            eventPublisher.publishPaymentRequest(order);
            
            // Publish order created event for notifications
            eventPublisher.publishOrderCreated(order);
            
        } catch (Exception e) {
            log.error("Failed to handle CreateOrderEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
            throw e; // Rethrow to ensure transaction rolls back and Axon retries
        }
    }

    @EventHandler
    public void on(OrderStatusUpdatedEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for status update", event.getOrderId());
            return;
        }

        // Update order status and timestamps using entity method
        order.updateStatus(event.getNewStatus(), event.getUpdatedAt());
        orderRepository.save(order);
        
        log.info("Order {} status updated from {} to {}", 
            event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());
        
        // Publish order status updated event for notifications
        eventPublisher.publishOrderStatusUpdated(
            order,
            event.getPreviousStatus() != null ? event.getPreviousStatus().name() : "UNKNOWN",
            event.getNewStatus().name(),
            event.getUpdatedBy()
        );
        
        // Publish order-completed event for loyalty points
        if (OrderConstants.STATUS_COMPLETED.equals(event.getNewStatus().name())) {
            String completedAt = event.getUpdatedAt() != null ? event.getUpdatedAt().toString() : null;
            eventPublisher.publishOrderCompleted(order, completedAt);
        } else if (com.project3.orderservice.command.enums.OrderStatus.READY.equals(event.getNewStatus())) {
             // CKECK AUTO-COMPLETE: If READY and PAID/SUCCESS -> COMPLETED
             checkAndAutoCompleteOrder(order);
        }
        
        // Publish delivery request if needed using entity method
        if (order.shouldTriggerDelivery()) {
            eventPublisher.publishDeliveryRequest(order);
        }
    }

    @EventHandler
    public void on(OrderCancelledEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for cancellation", event.getOrderId());
            return;
        }

        // Cancel order using entity method
        order.cancel(event.getCancellationReason(), event.getCancelledAt());

        orderRepository.save(order);
        log.info("Order {} cancelled: {}", event.getOrderId(), event.getCancellationReason());
        
        // Publish order cancelled event for notifications
        eventPublisher.publishOrderCancelled(order, event.getCancellationReason());
    }

    @EventHandler
    public void on(BillSplitEvent event) {
        log.info("Bill split event received for order {} into orders: {}", 
            event.getOriginalOrderId(), event.getNewOrderIds());
    }

    @EventHandler
    public void on(InventoryDeductionRequestEvent event) {
        log.info("Inventory deduction requested for order {}", event.getOrderId());

        try {
            Order order = orderRepository.findById(event.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Order {} not found for inventory deduction", event.getOrderId());
                return;
            }

            Map<String, Double> ingredientQuantities = ingredientExtractionService.extractIngredientQuantities(order.getOrderId());
            
            if (ingredientQuantities.isEmpty()) {
                log.warn("No ingredients found for order {}", event.getOrderId());
                return;
            }

            // Publish inventory deduction events for each ingredient
            for (Map.Entry<String, Double> entry : ingredientQuantities.entrySet()) {
                eventPublisher.publishInventoryDeduction(
                    event.getOrderId(), 
                    entry.getKey(), 
                    entry.getValue(), 
                    event.getReason()
                );
            }
            
            log.info("Inventory deduction requests sent via Kafka for order {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send inventory deduction requests for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    @EventHandler
    public void on(OrderPaymentUpdatedEvent event) {
        log.info("Handling OrderPaymentUpdatedEvent for order {}", event.getOrderId());
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order != null) {
            order.setPaymentId(event.getPaymentId());
            order.setPaymentStatus(event.getPaymentStatus());
            orderRepository.save(order);
            log.info("Updated order {} with paymentId {} and status {}", 
                event.getOrderId(), event.getPaymentId(), event.getPaymentStatus());
            
            // CHECK AUTO-COMPLETE
            checkAndAutoCompleteOrder(order);

        } else {
            log.warn("Order {} not found for payment update", event.getOrderId());
        }
    }
    
    @EventHandler
    public void on(OrderPaymentStatusUpdatedEvent event) {
        log.info("Handling OrderPaymentStatusUpdatedEvent for order {}", event.getOrderId());
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order != null) {
            order.setPaymentStatus(event.getPaymentStatus());
            orderRepository.save(order);
            log.info("Updated order {} payment status to {}", event.getOrderId(), event.getPaymentStatus());

            // CHECK AUTO-COMPLETE
            checkAndAutoCompleteOrder(order);

        } else {
            log.warn("Order {} not found for payment status update", event.getOrderId());
        }
    }

    private void checkAndAutoCompleteOrder(Order order) {
        boolean isReady = OrderStatus.READY.equals(order.getOrderStatus());
        boolean isPaid = "PAID".equalsIgnoreCase(order.getPaymentStatus()) || "SUCCESS".equalsIgnoreCase(order.getPaymentStatus());

        if (isReady && isPaid) {
            log.info("Auto-completing order {} as it is READY and PAID", order.getOrderId());
            commandGateway.send(new UpdateOrderStatusCommand(
                order.getOrderId(),
                OrderStatus.COMPLETED,
                "System (Auto-Complete)",
                "Auto-completed (Ready + Paid)"
            ));
        }
    }
}
