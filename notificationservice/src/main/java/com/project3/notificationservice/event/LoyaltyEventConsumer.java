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

@Component
@Slf4j
public class LoyaltyEventConsumer {
    
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
    @KafkaListener(topics = "loyalty-points-earned", groupId = "notification-service-group")
    public void handlePointsEarned(@Payload String message) {
        try {
            log.info("Received PointsEarnedEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String userId = (String) event.get("userId");
            Object pointsObj = event.get("points");
            String orderId = (String) event.get("orderId");
            String description = (String) event.get("description");
            Object pointsAfterObj = event.get("pointsAfter");
            
            Long points = pointsObj instanceof Number 
                ? ((Number) pointsObj).longValue() : null;
            Long pointsAfter = pointsAfterObj instanceof Number 
                ? ((Number) pointsAfterObj).longValue() : null;
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("points", points);
            metadata.put("pointsAfter", pointsAfter);
            metadata.put("orderId", orderId);
            metadata.put("description", description);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Notify customer about points earned
            String title = "Bạn đã nhận được điểm thưởng!";
            String notificationMessage = String.format(
                "Bạn đã nhận được %d điểm từ đơn hàng #%s. Tổng điểm hiện tại: %d điểm", 
                points != null ? points : 0, 
                orderId != null ? orderId : "N/A",
                pointsAfter != null ? pointsAfter : 0);
            
            if (userId != null && !userId.isEmpty() && points != null && points > 0) {
                notificationService.createNotification(
                    userId,
                    "LOYALTY_UPDATE",
                    title,
                    notificationMessage,
                    "LOW",
                    metadataJson
                );
                log.info("Created points earned notification for user {}: {} points", userId, points);
            }
            
        } catch (Exception e) {
            log.error("Error processing PointsEarnedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process PointsEarnedEvent", e);
        }
    }
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "loyalty-voucher-redeemed", groupId = "notification-service-group")
    public void handleVoucherRedeemed(@Payload String message) {
        try {
            log.info("Received VoucherRedeemedEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String userId = (String) event.get("userId");
            String voucherId = (String) event.get("voucherId");
            Object pointsRedeemedObj = event.get("pointsRedeemed");
            String orderId = (String) event.get("orderId");
            Object pointsAfterObj = event.get("pointsAfter");
            
            Long pointsRedeemed = pointsRedeemedObj instanceof Number 
                ? ((Number) pointsRedeemedObj).longValue() : null;
            Long pointsAfter = pointsAfterObj instanceof Number 
                ? ((Number) pointsAfterObj).longValue() : null;
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("voucherId", voucherId);
            metadata.put("pointsRedeemed", pointsRedeemed);
            metadata.put("pointsAfter", pointsAfter);
            metadata.put("orderId", orderId);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Notify customer about voucher redemption
            String title = "Voucher đã được sử dụng";
            String notificationMessage = String.format(
                "Bạn đã sử dụng voucher #%s. Đã trừ %d điểm. Điểm còn lại: %d điểm", 
                voucherId != null ? voucherId : "N/A",
                pointsRedeemed != null ? pointsRedeemed : 0,
                pointsAfter != null ? pointsAfter : 0);
            
            if (userId != null && !userId.isEmpty()) {
                notificationService.createNotification(
                    userId,
                    "LOYALTY_UPDATE",
                    title,
                    notificationMessage,
                    "LOW",
                    metadataJson
                );
                log.info("Created voucher redeemed notification for user {}: voucher {}", userId, voucherId);
            }
            
        } catch (Exception e) {
            log.error("Error processing VoucherRedeemedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process VoucherRedeemedEvent", e);
        }
    }
}

