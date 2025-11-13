package com.project3.commonservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemIngredientsResponseEvent {
    private String requestId;
    private String menuItemId;
    private List<String> ingredientIds;
    private Long timestamp;
}

