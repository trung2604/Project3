package com.project3.orderservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    private String menuItemId;
    private String name;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
    private String notes;
}

