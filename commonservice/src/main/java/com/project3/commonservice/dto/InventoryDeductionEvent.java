package com.project3.commonservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDeductionEvent {
    private String orderId;
    private String ingredientId;
    private Integer quantity;
    private String unit;
    private String reference;
    private String reason;
    private Long timestamp;
}

