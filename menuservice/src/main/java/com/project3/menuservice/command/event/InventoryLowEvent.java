package com.project3.menuservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryLowEvent {
    private String ingredientId;
    private String ingredientName;
    private Integer currentStock;
    private Integer minimumStock;
}
