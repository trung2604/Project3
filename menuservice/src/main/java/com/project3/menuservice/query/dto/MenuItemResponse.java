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
    private String categoryId;
    private String categoryName;
    private String description;
    private Double price;
    private Boolean active;
    private String imageUrl;
    private String imagePublicId;
    private Integer preparationTime;
    private String recipe;
    private List<String> ingredients;
    private List<MenuItemIngredientDTO> ingredientDetails;

    public static MenuItemResponse fromEntity(MenuItem item) {
        if (item == null) return null;
        MenuItemResponse dto = new MenuItemResponse();
        dto.setMenuItemId(item.getMenuItemId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setActive(item.getActive());
        dto.setImageUrl(item.getImageUrl());
        dto.setImagePublicId(item.getImagePublicId());
        dto.setPreparationTime(item.getPreparationTime());
        dto.setRecipe(item.getRecipe());
        
        try {
            if (item.getMenuItemIngredients() != null && !item.getMenuItemIngredients().isEmpty()) {
                List<MenuItemIngredientDTO> ingredientDetailsList = new ArrayList<>();
                List<String> legacyIngredientIds = new ArrayList<>();
                
                for (com.project3.menuservice.command.entity.MenuItemIngredient miIngredient : item.getMenuItemIngredients()) {
                    MenuItemIngredientDTO dtoItem = new MenuItemIngredientDTO();
                    dtoItem.setIngredientId(miIngredient.getIngredientId());
                    dtoItem.setQuantity(miIngredient.getQuantity());
                    dtoItem.setUnit(miIngredient.getUnit());
                    dtoItem.setNotes(miIngredient.getNotes());
                    ingredientDetailsList.add(dtoItem);
                    legacyIngredientIds.add(miIngredient.getIngredientId());
                }
                dto.setIngredientDetails(ingredientDetailsList);
                dto.setIngredients(legacyIngredientIds);
            } else {
                dto.setIngredientDetails(new ArrayList<>());
                try {
                    dto.setIngredients(new ArrayList<>(item.getIngredients()));
                } catch (Exception e) {
                    dto.setIngredients(new ArrayList<>());
                }
            }
        } catch (Exception e) {
            dto.setIngredientDetails(new ArrayList<>());
            try {
                dto.setIngredients(new ArrayList<>(item.getIngredients()));
            } catch (Exception e2) {
                dto.setIngredients(new ArrayList<>());
            }
        }
        
        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getCategoryId());
            dto.setCategoryName(item.getCategory().getName());
        }
        return dto;
    }
}


