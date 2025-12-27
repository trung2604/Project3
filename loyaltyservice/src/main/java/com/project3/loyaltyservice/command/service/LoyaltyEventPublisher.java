package com.project3.loyaltyservice.command.service;

import com.project3.commonservice.service.KafkaService;
import com.project3.loyaltyservice.command.constants.LoyaltyConstants;
import com.project3.loyaltyservice.command.events.PointsEarnedEvent;
import com.project3.loyaltyservice.command.events.VoucherRedeemedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing loyalty-related Kafka events
 * Encapsulates all Kafka event publishing logic for better cohesion
 */
@Service
@Slf4j
public class LoyaltyEventPublisher {
    
    @Autowired
    private KafkaService kafkaService;
    
    /**
     * Publishes points earned event to Kafka
     */
    public void publishPointsEarned(PointsEarnedEvent event) {
        try {
            Map<String, Object> pointsEvent = new HashMap<>();
            pointsEvent.put("accountId", event.getAccountId());
            pointsEvent.put("userId", event.getUserId());
            pointsEvent.put("points", event.getPoints());
            pointsEvent.put("pointsBefore", event.getPointsBefore());
            pointsEvent.put("pointsAfter", event.getPointsAfter());
            pointsEvent.put("orderId", event.getOrderId());
            pointsEvent.put("description", event.getDescription());
            pointsEvent.put("earnedAt", event.getEarnedAt() != null ? event.getEarnedAt().toString() : "");
            pointsEvent.put("timestamp", System.currentTimeMillis());
            
            kafkaService.sendMessage(LoyaltyConstants.TOPIC_POINTS_EARNED, pointsEvent);
            log.info("Points earned event published for user {}: {} points", 
                event.getUserId(), event.getPoints());
        } catch (Exception e) {
            log.error("Failed to publish points earned event for user {}: {}", 
                event.getUserId(), e.getMessage(), e);
        }
    }
    
    /**
     * Publishes voucher redeemed event to Kafka
     */
    public void publishVoucherRedeemed(VoucherRedeemedEvent event) {
        try {
            Map<String, Object> voucherEvent = new HashMap<>();
            voucherEvent.put("accountId", event.getAccountId());
            voucherEvent.put("userId", event.getUserId());
            voucherEvent.put("voucherId", event.getVoucherId());
            voucherEvent.put("pointsRedeemed", event.getPointsRedeemed());
            voucherEvent.put("pointsBefore", event.getPointsBefore());
            voucherEvent.put("pointsAfter", event.getPointsAfter());
            voucherEvent.put("orderId", event.getOrderId());
            voucherEvent.put("redeemedAt", event.getRedeemedAt() != null ? event.getRedeemedAt().toString() : "");
            voucherEvent.put("timestamp", System.currentTimeMillis());
            
            kafkaService.sendMessage(LoyaltyConstants.TOPIC_VOUCHER_REDEEMED, voucherEvent);
            log.info("Voucher redeemed event published for user {}: voucher {}", 
                event.getUserId(), event.getVoucherId());
        } catch (Exception e) {
            log.error("Failed to publish voucher redeemed event for user {}: {}", 
                event.getUserId(), e.getMessage(), e);
        }
    }
}

