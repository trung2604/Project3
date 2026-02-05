package com.project3.orderservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.orderservice.command.commands.UpdateOrderPaymentStatusCommand;
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
 * Kafka consumer for payment-refunded events from PaymentService
 * Automatically updates order status to "REFUNDED" or PARTIALLY_REFUNDED
 */
@Component
@Slf4j
public class PaymentRefundedKafkaConsumer {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "payment-refunded", groupId = "order-service-group")
    public void handlePaymentRefunded(@Payload String message) {
        try {
            log.info("Received payment-refunded event: {}", message);
            
            Map<String, Object> paymentData = objectMapper.readValue(message, Map.class);
            
            String orderId = (String) paymentData.get("orderId");
            String statusStr = (String) paymentData.get("status");
            String reason = (String) paymentData.get("reason");
            Object refundAmountObj = paymentData.get("refundAmount");
            
            if (orderId == null || orderId.isEmpty()) {
                log.error("Invalid payment-refunded event: missing orderId");
                return;
            }
            
            Double refundAmount = refundAmountObj instanceof Number 
                ? ((Number) refundAmountObj).doubleValue() 
                : Double.parseDouble(refundAmountObj.toString());
            
            // Update Payment Status in Order
            String newPaymentStatus = "REFUNDED".equals(statusStr) ? "REFUNDED" : "PARTIALLY_REFUNDED";
            
            UpdateOrderPaymentStatusCommand statusCommand =
                UpdateOrderPaymentStatusCommand.builder()
                .orderId(orderId)
                .paymentStatus(newPaymentStatus)
                .build();
            commandGateway.sendAndWait(statusCommand);
            log.info("Order {} payment status updated to {}", orderId, newPaymentStatus);
            
        } catch (Exception e) {
            log.error("Error processing payment-refunded event: {}", e.getMessage(), e);
        }
    }
}
