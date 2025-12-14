package com.project3.loyaltyservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.loyaltyservice.command.commands.EarnPointsCommand;
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

@Component
@Slf4j
public class OrderCompletedKafkaConsumer {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private LoyaltyAccountService loyaltyAccountService;
    
    @Autowired
    private PointsCalculator pointsCalculator;
    
    @Autowired
    private OrderDataParser orderDataParser;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "order-completed", groupId = "loyalty-service-group")
    public void handleOrderCompleted(@Payload String message) {
        try {
            log.info("Received order completed event: {}", message);
            
            Map<String, Object> orderData = objectMapper.readValue(message, Map.class);
            OrderDataParser.OrderData parsedData = orderDataParser.parse(orderData);
            
            if (parsedData == null) {
                return; // Invalid data, already logged
            }
            
            // Find or create loyalty account
            LoyaltyAccount account = loyaltyAccountService.findOrCreateAccount(parsedData.getCustomerId());
            
            // Calculate and award points
            Long points = pointsCalculator.calculatePoints(parsedData.getTotalAmount());
            String description = pointsCalculator.generatePointsDescription(
                parsedData.getOrderId(), 
                parsedData.getTotalAmount()
            );
            
            EarnPointsCommand command = new EarnPointsCommand();
            command.setAccountId(account.getAccountId());
            command.setUserId(parsedData.getCustomerId());
            command.setPoints(points);
            command.setOrderId(parsedData.getOrderId());
            command.setDescription(description);
            
            commandGateway.sendAndWait(command);
            log.info("Points earned for user {}: {} points from order {}", 
                parsedData.getCustomerId(), points, parsedData.getOrderId());
            
        } catch (Exception e) {
            log.error("Error processing order completed event: {}", e.getMessage(), e);
        }
    }
}

