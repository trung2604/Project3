package com.project3.orderservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.orderservice.command.commands.UpdateOrderStatusCommand;
import com.project3.orderservice.command.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for payment-failed events from PaymentService
 * Automatically updates order status to PAYMENT_FAILED when payment fails
 */
@Component
@Slf4j
public class PaymentFailedKafkaConsumer {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "payment-failed", groupId = "order-service-group")
    public void handlePaymentFailed(@Payload String message) {
        try {
            log.info("Received payment-failed event: {}", message);
            
            Map<String, Object> paymentData = objectMapper.readValue(message, Map.class);
            
            String orderId = (String) paymentData.get("orderId");
            String paymentId = (String) paymentData.get("paymentId");
            String failureReason = (String) paymentData.get("failureReason");
            
            if (orderId == null || orderId.isEmpty()) {
                log.error("Invalid payment-failed event: missing orderId");
                return;
            }
            
            // Update Payment Status in Order to FAILED
            com.project3.orderservice.command.commands.UpdateOrderPaymentStatusCommand statusCommand = 
                com.project3.orderservice.command.commands.UpdateOrderPaymentStatusCommand.builder()
                .orderId(orderId)
                .paymentStatus("FAILED")
                .build();
            commandGateway.sendAndWait(statusCommand);
            log.info("Order {} payment status updated to FAILED", orderId);
            
        } catch (Exception e) {
            log.error("Error processing payment-failed event: {}", e.getMessage(), e);
        }
    }
}
