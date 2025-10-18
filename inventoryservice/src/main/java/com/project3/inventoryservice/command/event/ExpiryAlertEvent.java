package com.project3.inventoryservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpiryAlertEvent {
    private String ingredientId;
    private String alertId;
    private String ingredientName;
    private String expiryDate;
    private String severity;
    private String message;
    private LocalDateTime alertDate;
}
