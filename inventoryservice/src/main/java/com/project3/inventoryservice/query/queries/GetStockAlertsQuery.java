package com.project3.inventoryservice.query.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetStockAlertsQuery {
    private String alertType;
    private String severity;
    private Boolean isActive;
}
