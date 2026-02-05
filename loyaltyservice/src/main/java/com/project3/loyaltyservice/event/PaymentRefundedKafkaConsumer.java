package com.project3.loyaltyservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.loyaltyservice.command.commands.DeductPointsCommand;
import com.project3.loyaltyservice.command.entity.LoyaltyAccount;
import com.project3.loyaltyservice.command.service.LoyaltyAccountService;
import com.project3.loyaltyservice.command.service.PointsCalculator;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for payment-refunded events from PaymentService
 * Automatically deducts loyalty points when a payment is refunded
 */
@Component
@Slf4j
public class PaymentRefundedKafkaConsumer {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private LoyaltyAccountService loyaltyAccountService;
    
    @Autowired
    private PointsCalculator pointsCalculator;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "payment-refunded", groupId = "loyalty-service-group")
    public void handlePaymentRefunded(@Payload String message) {
        try {
            log.info("Received payment-refunded event: {}", message);
            
            Map<String, Object> paymentData = objectMapper.readValue(message, Map.class);
            
            String orderId = (String) paymentData.get("orderId");
            String customerId = (String) paymentData.get("customerId");
            String paymentId = (String) paymentData.get("paymentId");
            
            Object originalAmountObj = paymentData.get("originalAmount");
            Object refundAmountObj = paymentData.get("refundAmount");
            String status = (String) paymentData.get("status");
            String reason = (String) paymentData.get("reason");
            
            if (customerId == null || customerId.isEmpty()) {
                log.error("Invalid payment-refunded event: missing customerId");
                return;
            }
            
            if (refundAmountObj == null) {
                log.error("Invalid payment-refunded event: missing refundAmount");
                return;
            }
            
            Double refundAmount = refundAmountObj instanceof Number 
                ? ((Number) refundAmountObj).doubleValue() 
                : Double.parseDouble(refundAmountObj.toString());
            
            // Find loyalty account
            LoyaltyAccount account = loyaltyAccountService.findOrCreateAccount(customerId);
            
            // Calculate points to deduct based on refund amount
            Long pointsToDeduct = pointsCalculator.calculatePoints(refundAmount);
            
            // Only deduct if there are points to deduct
            if (pointsToDeduct > 0) {
                DeductPointsCommand command = new DeductPointsCommand();
                command.setAccountId(account.getAccountId());
                command.setUserId(customerId);
                command.setPoints(pointsToDeduct);
                command.setOrderId(orderId);
                command.setReason("Payment refunded: " + (reason != null ? reason : "Refund amount: " + refundAmount));
                
                commandGateway.sendAndWait(command);
                log.info("Deducted {} points from user {} due to payment refund. PaymentId: {}, OrderId: {}", 
                    pointsToDeduct, customerId, paymentId, orderId);
            } else {
                log.info("No points to deduct for refund amount: {}", refundAmount);
            }
            
        } catch (Exception e) {
            log.error("Error processing payment-refunded event: {}", e.getMessage(), e);
        }
    }
}
