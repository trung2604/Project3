package com.project3.paymentservice.command.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.commonservice.service.KafkaService;
import com.project3.paymentservice.command.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PaymentEventPublisher {
    
    @Autowired
    private KafkaService kafkaService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public void publishPaymentCreated(Payment payment) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getPaymentId());
            event.put("orderId", payment.getOrderId());
            event.put("customerId", payment.getCustomerId());
            event.put("amount", payment.getAmount());
            event.put("paymentMethod", payment.getPaymentMethod().toString());
            event.put("status", "PENDING"); // Initial status
            event.put("createdAt", payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null);
            event.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(event);
            
            // Publish to OrderService
            kafkaService.sendMessage("payment-created", message);
            log.info("Published payment-created event to Kafka for orderId: {}", payment.getOrderId());
            
        } catch (Exception e) {
            log.error("Error publishing payment created event: {}", e.getMessage(), e);
        }
    }
    
    public void publishPaymentCompleted(Payment payment) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getPaymentId());
            event.put("orderId", payment.getOrderId());
            event.put("customerId", payment.getCustomerId());
            event.put("amount", payment.getAmount());
            event.put("paymentMethod", payment.getPaymentMethod().toString());
            event.put("status", "SUCCESS");
            event.put("transactionReference", payment.getTransactionReference());
            event.put("processedAt", payment.getProcessedAt() != null ? payment.getProcessedAt().toString() : null);
            event.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(event);
            
            // Publish to OrderService and LoyaltyService
            kafkaService.sendMessage("payment-completed", message);
            log.info("Published payment-completed event to Kafka for orderId: {}", payment.getOrderId());
            
        } catch (Exception e) {
            log.error("Error publishing payment completed event: {}", e.getMessage(), e);
        }
    }
    
    public void publishPaymentFailed(Payment payment) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getPaymentId());
            event.put("orderId", payment.getOrderId());
            event.put("customerId", payment.getCustomerId());
            event.put("amount", payment.getAmount());
            event.put("paymentMethod", payment.getPaymentMethod().toString());
            event.put("status", "FAILED");
            event.put("failureReason", payment.getFailureReason());
            event.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(event);
            
            // Publish to OrderService
            kafkaService.sendMessage("payment-failed", message);
            log.info("Published payment-failed event to Kafka for orderId: {}", payment.getOrderId());
            
        } catch (Exception e) {
            log.error("Error publishing payment failed event: {}", e.getMessage(), e);
        }
    }
    
    public void publishPaymentRefunded(Payment payment, Double refundAmount, String reason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getPaymentId());
            event.put("orderId", payment.getOrderId());
            event.put("customerId", payment.getCustomerId());
            event.put("originalAmount", payment.getAmount());
            event.put("refundAmount", refundAmount);
            event.put("totalRefunded", payment.getRefundedAmount());
            event.put("status", payment.getStatus().toString());
            event.put("reason", reason);
            event.put("refundedAt", payment.getRefundedAt() != null ? payment.getRefundedAt().toString() : null);
            event.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(event);
            
            // Publish to OrderService and LoyaltyService
            kafkaService.sendMessage("payment-refunded", message);
            log.info("Published payment-refunded event to Kafka for orderId: {}", payment.getOrderId());
            
        } catch (Exception e) {
            log.error("Error publishing payment refunded event: {}", e.getMessage(), e);
        }
    }
}
