package com.project3.paymentservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.paymentservice.command.commands.CreatePaymentCommand;
import com.project3.paymentservice.command.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PaymentRequestKafkaConsumer {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "payment-request", groupId = "payment-service-group")
    public void handlePaymentRequest(@Payload String message) {
        try {
            log.info("Received payment request event: {}", message);
            
            Map<String, Object> requestData = objectMapper.readValue(message, Map.class);
            
            String orderId = (String) requestData.get("orderId");
            String customerId = (String) requestData.get("customerId");
            Object amountObj = requestData.get("amount");
            String paymentMethodStr = (String) requestData.get("paymentMethod");
            
            if (orderId == null || customerId == null || amountObj == null || paymentMethodStr == null) {
                log.error("Invalid payment request data: missing required fields");
                return;
            }
            
            Double amount = amountObj instanceof Number 
                ? ((Number) amountObj).doubleValue() 
                : Double.parseDouble(amountObj.toString());
            
            PaymentMethod paymentMethod;
            try {
                paymentMethod = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid payment method: {}", paymentMethodStr);
                return;
            }
            
            // Create payment command
            CreatePaymentCommand command = new CreatePaymentCommand();
            command.setPaymentId(UUID.randomUUID().toString());
            command.setOrderId(orderId);
            command.setCustomerId(customerId);
            command.setAmount(amount);
            command.setPaymentMethod(paymentMethod);
            
            commandGateway.sendAndWait(command);
            log.info("Payment created from Kafka event for orderId: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error processing payment request event: {}", e.getMessage(), e);
        }
    }
}
