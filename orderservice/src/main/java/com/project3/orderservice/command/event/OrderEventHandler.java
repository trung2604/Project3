package com.project3.orderservice.command.event;

import com.project3.commonservice.dto.DeliveryRequestEvent;
import com.project3.commonservice.dto.InventoryDeductionEvent;
import com.project3.commonservice.dto.PaymentRequestEvent;
import com.project3.commonservice.service.KafkaService;
import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.entity.OrderItem;
import com.project3.orderservice.command.entity.OrderItemRepository;
import com.project3.orderservice.command.entity.OrderRespository;
import com.project3.orderservice.command.service.MenuServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class OrderEventHandler {
    
    @Autowired
    private OrderRespository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private KafkaService kafkaService;
    
    @Autowired
    private MenuServiceClient menuServiceClient;

    @EventHandler
    public void on(CreateOrderEvent event) {
        if (orderRepository.existsById(event.getOrderId())) {
            log.warn("Order {} already exists", event.getOrderId());
            return;
        }

        Order order = new Order();
        order.setOrderId(event.getOrderId());
        order.setCustomerId(event.getCustomerId());
        order.setCustomerName(event.getCustomerName());
        order.setCustomerPhone(event.getCustomerPhone());
        order.setOrderType(event.getOrderType());
        order.setOrderStatus(event.getOrderStatus());
        order.setSubtotal(event.getSubtotal());
        order.setDiscountAmount(event.getDiscountAmount());
        order.setDiscountPercentage(event.getDiscountPercentage());
        order.setVatAmount(event.getVatAmount());
        order.setVatPercentage(event.getVatPercentage());
        order.setTotalAmount(event.getTotalAmount());
        order.setOrderDate(event.getOrderDate());
        order.setDeliveryAddress(event.getDeliveryAddress());
        order.setTableNumber(event.getTableNumber());
        order.setNotes(event.getNotes());
        order.setCreatedBy(event.getCreatedBy());

        orderRepository.save(order);

        if (event.getOrderItems() != null && !event.getOrderItems().isEmpty()) {
            List<OrderItem> orderItems = new ArrayList<>();
            for (com.project3.orderservice.command.dto.OrderItemDTO itemDTO : event.getOrderItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderItemId(UUID.randomUUID().toString());
                orderItem.setOrderId(event.getOrderId());
                orderItem.setMenuItemId(itemDTO.getMenuItemId());
                orderItem.setName(itemDTO.getName());
                orderItem.setQuantity(itemDTO.getQuantity());
                orderItem.setUnitPrice(itemDTO.getUnitPrice());
                orderItem.setSubtotal(itemDTO.getSubtotal());
                orderItem.setNotes(itemDTO.getNotes());
                orderItems.add(orderItem);
            }
            orderItemRepository.saveAll(orderItems);
        }

        log.info("Order {} created successfully", event.getOrderId());
        
        try {
            PaymentRequestEvent paymentEvent = new PaymentRequestEvent();
            paymentEvent.setOrderId(event.getOrderId());
            paymentEvent.setAmount(event.getTotalAmount());
            paymentEvent.setCustomerId(event.getCustomerId());
            paymentEvent.setOrderType(event.getOrderType().name());
            paymentEvent.setTimestamp(System.currentTimeMillis());
            
            kafkaService.sendMessage("payment-request", paymentEvent);
            log.info("Payment request sent via Kafka for order {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send payment request for order {}: {}", event.getOrderId(), e.getMessage());
        }
    }

    @EventHandler
    public void on(OrderStatusUpdatedEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for status update", event.getOrderId());
            return;
        }

        order.setOrderStatus(event.getNewStatus());
        
        if (event.getNewStatus().name().equals("COOKING") && order.getCookingStartTime() == null) {
            order.setCookingStartTime(event.getUpdatedAt());
        } else if (event.getNewStatus().name().equals("READY")) {
            order.setReadyTime(event.getUpdatedAt());
        } else if (event.getNewStatus().name().equals("COMPLETED")) {
            order.setCompletedTime(event.getUpdatedAt());
        }

        orderRepository.save(order);
        log.info("Order {} status updated from {} to {}", 
            event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());
        
        if (event.getNewStatus().name().equals("READY") && 
            order.getOrderType().name().equals("DELIVERY")) {
            try {
                DeliveryRequestEvent deliveryEvent = new DeliveryRequestEvent();
                deliveryEvent.setOrderId(event.getOrderId());
                deliveryEvent.setDeliveryAddress(order.getDeliveryAddress());
                deliveryEvent.setCustomerPhone(order.getCustomerPhone());
                deliveryEvent.setCustomerName(order.getCustomerName());
                deliveryEvent.setTotalAmount(order.getTotalAmount());
                deliveryEvent.setTimestamp(System.currentTimeMillis());
                
                kafkaService.sendMessage("delivery-request", deliveryEvent);
                log.info("Delivery request sent via Kafka for order {}", event.getOrderId());
            } catch (Exception e) {
                log.error("Failed to send delivery request for order {}: {}", event.getOrderId(), e.getMessage());
            }
        }
    }

    @EventHandler
    public void on(OrderCancelledEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for cancellation", event.getOrderId());
            return;
        }

        order.setOrderStatus(com.project3.orderservice.command.enums.OrderStatus.CANCELLED);
        order.setCancelledTime(event.getCancelledAt());
        order.setCancellationReason(event.getCancellationReason());

        orderRepository.save(order);
        log.info("Order {} cancelled: {}", event.getOrderId(), event.getCancellationReason());
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

            Map<String, Integer> ingredientQuantities = extractIngredientQuantities(order.getOrderId());
            
            if (ingredientQuantities.isEmpty()) {
                log.warn("No ingredients found for order {}", event.getOrderId());
                return;
            }

            for (Map.Entry<String, Integer> entry : ingredientQuantities.entrySet()) {
                InventoryDeductionEvent deductionEvent = new InventoryDeductionEvent();
                deductionEvent.setOrderId(event.getOrderId());
                deductionEvent.setIngredientId(entry.getKey());
                deductionEvent.setQuantity(entry.getValue());
                deductionEvent.setUnit("kg");
                deductionEvent.setReference(event.getOrderId());
                deductionEvent.setReason(event.getReason() != null ? event.getReason() : "Order cooking - automatic deduction");
                deductionEvent.setTimestamp(System.currentTimeMillis());
                
                kafkaService.sendMessage("inventory-deduction-request", deductionEvent);
            }
            
            log.info("Inventory deduction requests sent via Kafka for order {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send inventory deduction requests for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    private Map<String, Integer> extractIngredientQuantities(String orderId) {
        Map<String, Integer> ingredientQuantities = new HashMap<>();
        
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem orderItem : orderItems) {
            List<String> ingredients = menuServiceClient.getMenuItemIngredients(orderItem.getMenuItemId());
            int quantity = orderItem.getQuantity();
            
            for (String ingredientId : ingredients) {
                ingredientQuantities.merge(ingredientId, quantity, Integer::sum);
            }
        }
        
        return ingredientQuantities;
    }
}
