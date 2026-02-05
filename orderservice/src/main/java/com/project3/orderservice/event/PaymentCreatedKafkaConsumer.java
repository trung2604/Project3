package com.project3.orderservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.orderservice.command.commands.UpdateOrderPaymentCommand;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class PaymentCreatedKafkaConsumer {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-created", groupId = "order-service-group")
    public void handlePaymentCreated(@Payload String message) {
        try {
            log.info("Received PaymentCreatedEvent: {}", message);

            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);

            String orderId = (String) event.get("orderId");
            String paymentId = (String) event.get("paymentId");
            String status = (String) event.get("status");
            
            if (orderId == null || paymentId == null) {
                log.error("Invalid PaymentCreatedEvent: missing orderId or paymentId");
                return;
            }

            // Send command to update Order with paymentId
            UpdateOrderPaymentCommand command = UpdateOrderPaymentCommand.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .paymentStatus(status != null ? status : "PENDING")
                .build();
            
            commandGateway.sendAndWait(command);
            log.info("Updated order {} with paymentId {}", orderId, paymentId);

        } catch (Exception e) {
            log.error("Error processing PaymentCreatedEvent: {}", e.getMessage(), e);
        }
    }
}
