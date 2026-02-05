package com.project3.notificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer for payment-related events
 * Sends notifications to customers when payment status changes
 */
@Component
@Slf4j
public class PaymentEventConsumer {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "payment-completed", groupId = "notification-service-group")
    public void handlePaymentCompleted(@Payload String message) {
        try {
            log.info("Received PaymentCompletedEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String customerId = (String) event.get("customerId");
            Object amountObj = event.get("amount");
            String paymentMethod = (String) event.get("paymentMethod");
            
            Double amount = amountObj instanceof Number 
                ? ((Number) amountObj).doubleValue() : null;
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paymentId", paymentId);
            metadata.put("orderId", orderId);
            metadata.put("amount", amount);
            metadata.put("paymentMethod", paymentMethod);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Notify customer about successful payment
            String title = "Thanh toán thành công";
            String notificationMessage = String.format(
                "Thanh toán cho đơn hàng #%s đã thành công qua %s. Số tiền: %,.0f VNĐ",
                orderId, 
                paymentMethod != null ? paymentMethod : "Payment Gateway",
                amount != null ? amount : 0);
            
            if (customerId != null && !customerId.isEmpty()) {
                notificationService.createNotification(
                    customerId,
                    "PAYMENT_UPDATE",
                    title,
                    notificationMessage,
                    "HIGH",
                    metadataJson
                );
                log.info("Created payment success notification for customer {}", customerId);
            }
            
        } catch (Exception e) {
            log.error("Error processing PaymentCompletedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process PaymentCompletedEvent", e);
        }
    }
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "payment-failed", groupId = "notification-service-group")
    public void handlePaymentFailed(@Payload String message) {
        try {
            log.info("Received PaymentFailedEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String customerId = (String) event.get("customerId");
            Object amountObj = event.get("amount");
            String paymentMethod = (String) event.get("paymentMethod");
            String failureReason = (String) event.get("failureReason");
            
            Double amount = amountObj instanceof Number 
                ? ((Number) amountObj).doubleValue() : null;
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paymentId", paymentId);
            metadata.put("orderId", orderId);
            metadata.put("amount", amount);
            metadata.put("paymentMethod", paymentMethod);
            metadata.put("failureReason", failureReason);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Notify customer about failed payment
            String title = "Thanh toán thất bại";
            String notificationMessage = String.format(
                "Thanh toán cho đơn hàng #%s đã thất bại. Lý do: %s. Vui lòng thử lại hoặc chọn phương thức thanh toán khác.",
                orderId,
                failureReason != null ? failureReason : "Không xác định");
            
            if (customerId != null && !customerId.isEmpty()) {
                notificationService.createNotification(
                    customerId,
                    "PAYMENT_UPDATE",
                    title,
                    notificationMessage,
                    "HIGH",
                    metadataJson
                );
                log.info("Created payment failure notification for customer {}", customerId);
            }
            
        } catch (Exception e) {
            log.error("Error processing PaymentFailedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process PaymentFailedEvent", e);
        }
    }
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "payment-refunded", groupId = "notification-service-group")
    public void handlePaymentRefunded(@Payload String message) {
        try {
            log.info("Received PaymentRefundedEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String paymentId = (String) event.get("paymentId");
            String orderId = (String) event.get("orderId");
            String customerId = (String) event.get("customerId");
            Object originalAmountObj = event.get("originalAmount");
            Object refundAmountObj = event.get("refundAmount");
            String status = (String) event.get("status");
            String reason = (String) event.get("reason");
            
            Double originalAmount = originalAmountObj instanceof Number 
                ? ((Number) originalAmountObj).doubleValue() : null;
            Double refundAmount = refundAmountObj instanceof Number 
                ? ((Number) refundAmountObj).doubleValue() : null;
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paymentId", paymentId);
            metadata.put("orderId", orderId);
            metadata.put("originalAmount", originalAmount);
            metadata.put("refundAmount", refundAmount);
            metadata.put("status", status);
            metadata.put("reason", reason);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Notify customer about refund
            String title = "REFUNDED".equals(status) ? "Hoàn tiền toàn bộ" : "Hoàn tiền một phần";
            String notificationMessage = String.format(
                "Đơn hàng #%s: Đã hoàn %,.0f VNĐ%s. Lý do: %s",
                orderId,
                refundAmount != null ? refundAmount : 0,
                "REFUNDED".equals(status) ? " (toàn bộ)" : "",
                reason != null ? reason : "Không xác định");
            
            if (customerId != null && !customerId.isEmpty()) {
                notificationService.createNotification(
                    customerId,
                    "PAYMENT_UPDATE",
                    title,
                    notificationMessage,
                    "MEDIUM",
                    metadataJson
                );
                log.info("Created payment refund notification for customer {}", customerId);
            }
            
        } catch (Exception e) {
            log.error("Error processing PaymentRefundedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process PaymentRefundedEvent", e);
        }
    }
}
