package com.project3.commonservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemIngredientsRequestEvent {
    private String requestId;
    private String menuItemId;
    private String responseTopic;
    private Long timestamp;
}

