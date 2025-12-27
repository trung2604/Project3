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
    private Integer quantity; // Legacy: kept for backward compatibility
    private Double quantityDouble; // New: for precise quantities (e.g., 0.2 kg)
    private String unit;
    private String reference;
    private String reason;
    private Long timestamp;
    
    /**
     * Gets the quantity as Double, preferring quantityDouble if available
     */
    public Double getQuantityAsDouble() {
        if (quantityDouble != null) {
            return quantityDouble;
        }
        return quantity != null ? quantity.doubleValue() : 0.0;
    }
}

