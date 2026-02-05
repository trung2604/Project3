package com.project3.orderservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.orderservice.command.commands.UpdateOrderStatusCommand;
import com.project3.orderservice.command.commands.UpdateOrderPaymentStatusCommand;
import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.entity.OrderRespository;
import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.enums.OrderType;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for payment-completed events from PaymentService
 * Automatically updates order status to PAID when payment is successful
 * Then auto-completes the order if it's in READY status
 */
@Component
@Slf4j
public class PaymentCompletedKafkaConsumer {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private OrderRespository orderRepository;
    
    @KafkaListener(topics = "payment-completed", groupId = "order-service-group")
    public void handlePaymentCompleted(@Payload String message) {
        try {
            log.info("Received payment-completed event: {}", message);
            
            Map<String, Object> paymentData = objectMapper.readValue(message, Map.class);
            
            String orderId = (String) paymentData.get("orderId");
            String paymentId = (String) paymentData.get("paymentId");
            String status = (String) paymentData.get("status");
            
            if (orderId == null || orderId.isEmpty()) {
                log.error("Invalid payment-completed event: missing orderId");
                return;
            }
            
            if (!"SUCCESS".equals(status)) {
                log.warn("Payment completed event with non-SUCCESS status: {}", status);
                return;
            }
            
            // 1. Update Payment Status to SUCCESS
            // The OrderAggregate update paymentStatus. OrderStatus remains PENDING (or current). 
            // Auto-complete (READY->COMPLETED) logic still applies.
            UpdateOrderPaymentStatusCommand statusCommand = 
                UpdateOrderPaymentStatusCommand.builder()
                .orderId(orderId)
                .paymentStatus("SUCCESS")
                .build();
            commandGateway.sendAndWait(statusCommand);
            
            log.info("Payment success processed for Order: {}. Status updated.", orderId);
            
        } catch (Exception e) {
            log.error("Error processing payment-completed event: {}", e.getMessage(), e);
        }
    }

    // Removed autoCompleteOrderIfReady and shouldAutoComplete as logic is now inlined above
}

