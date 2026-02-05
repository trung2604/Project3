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
public class OrderCreatedKafkaConsumer {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "order-created", groupId = "payment-service-group")
    public void handleOrderCreated(@Payload String message) {
        try {
            log.info("Received OrderCreatedEvent: {}", message);

            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);

            String orderId = (String) event.get("orderId");
            String customerId = (String) event.get("customerId");
            Object totalAmountObj = event.get("totalAmount");
            String orderType = (String) event.get("orderType");
            
            if (orderId == null || totalAmountObj == null) {
                log.error("Invalid OrderCreatedEvent: missing orderId or totalAmount");
                return;
            }

            Double amount = totalAmountObj instanceof Number 
                ? ((Number) totalAmountObj).doubleValue() 
                : Double.parseDouble(totalAmountObj.toString());

            // Create Payment with PENDING status
            // Assuming default payment method CASH initially, user can change later?
            // Or "PENDING" payment doesn't need method yet?
            // CreatePaymentCommand requires PaymentMethod.
            // Let's assume CASH as default placeholder or PENDING method if supported.
            // For now use CASH as default if not provided (Wait, frontend selects method?)
            // Actually, when Order is created, User hasn't selected Payment Method yet (unless prepaid).
            // But we need a Payment record to show "Pay Now" button which updates it?
            // ProcessPaymentModal asks for method.
            // So we Create Payment with CASH (or explicit UNKNOWN if possible).
            // Let's use CASH for now or check if we can add UNKNOWN.
            
            // Disable auto-creation of payment to allow explicit creation with selected method (CASH/VIETQR/PAYPAL)
            // CreatePaymentCommand command = new CreatePaymentCommand();
            // command.setPaymentId(UUID.randomUUID().toString());
            // command.setOrderId(orderId);
            // command.setCustomerId(customerId != null ? customerId : "GUEST");
            // command.setAmount(amount);
            // command.setPaymentMethod(PaymentMethod.CASH); 
            
            // commandGateway.sendAndWait(command);
            // log.info("Initiated Payment Creation for Order: {}", orderId);
            log.info("Skipping auto-payment creation for Order: {}. Waiting for explicit payment creation.", orderId);

        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent: {}", e.getMessage(), e);
        }
    }
}
