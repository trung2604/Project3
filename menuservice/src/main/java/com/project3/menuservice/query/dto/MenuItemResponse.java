package com.project3.menuservice.query.dto;

import com.project3.menuservice.command.entity.MenuItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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

    public static MenuItemResponse fromEntity(MenuItem item) {
        if (item == null) return null;
        MenuItemResponse dto = new MenuItemResponse();
        dto.setMenuItemId(item.getMenuItemId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setActive(item.getActive());
        
        // Safely handle ingredients to avoid lazy loading issues
        try {
            dto.setIngredients(new ArrayList<>(item.getIngredients()));
        } catch (Exception e) {
            // If lazy loading fails, set empty list
            dto.setIngredients(new ArrayList<>());
        }
        
        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getCategoryId());
            dto.setCategoryName(item.getCategory().getName());
        }
        return dto;
    }
}


