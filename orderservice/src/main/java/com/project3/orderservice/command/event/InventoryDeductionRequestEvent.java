package com.project3.orderservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDeductionRequestEvent {
    private String orderId;
    private Map<String, Integer> ingredientQuantities;
    private String reason;
}

