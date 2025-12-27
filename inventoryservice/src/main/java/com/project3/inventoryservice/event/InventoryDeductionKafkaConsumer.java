package com.project3.inventoryservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.commonservice.dto.InventoryDeductionEvent;
import com.project3.inventoryservice.command.commands.StockOutCommand;
import com.project3.inventoryservice.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class InventoryDeductionKafkaConsumer {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "inventory-deduction-request", groupId = "inventory-service-group")
    public void handleInventoryDeductionRequest(@Payload String message) {
        try {
            log.info("Received inventory deduction request: {}", message);
            
            InventoryDeductionEvent event = objectMapper.readValue(message, InventoryDeductionEvent.class);
            
            StockOutCommand command = new StockOutCommand();
            command.setIngredientId(event.getIngredientId());
            // Use quantityDouble if available (for precise quantities), otherwise fallback to quantity
            Double quantity = event.getQuantityAsDouble();
            command.setQuantity(quantity != null ? quantity : 0.0);
            command.setUnit(event.getUnit() != null ? event.getUnit() : "kg");
            command.setTransactionId(IdGenerator.generateStockOutId());
            command.setTransactionDate(LocalDateTime.now());
            command.setReference(event.getReference() != null ? event.getReference() : event.getOrderId());
            command.setReason(event.getReason() != null ? event.getReason() : "Order cooking - automatic deduction");
            command.setCreatedBy("system");
            
            String result = commandGateway.sendAndWait(command);
            log.info("Inventory deduction completed for ingredient {}: {}", event.getIngredientId(), result);
            
        } catch (Exception e) {
            log.error("Error processing inventory deduction request: {}", e.getMessage(), e);
        }
    }
}

