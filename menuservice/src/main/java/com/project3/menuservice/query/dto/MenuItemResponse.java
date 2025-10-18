package com.project3.menuservice.query.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemResponse {
    private String menuItemId;
    private String name;
    private String categoryId; // Changed from category to categoryId
    private String categoryName; // Added category name for display
    private String description;
    private Double price;
    private Boolean active;
    private List<String> ingredients;
}


