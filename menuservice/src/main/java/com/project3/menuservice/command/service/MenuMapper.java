package com.project3.menuservice.command.service;

import com.project3.menuservice.command.entity.Category;
import com.project3.menuservice.command.entity.Combo;
import com.project3.menuservice.command.entity.MenuItem;
import com.project3.menuservice.command.event.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Mapper for converting between Menu entities and Events
 * Encapsulates mapping logic for better cohesion
 */
@Component
public class MenuMapper {
    
    /**
     * Maps MenuItemCreatedEvent to MenuItem entity
     */
    public MenuItem toMenuItemEntity(MenuItemCreatedEvent event, Category category) {
        MenuItem item = new MenuItem();
        item.setMenuItemId(event.getMenuItemId());
        item.setName(event.getName());
        item.setCategory(category);
        item.setDescription(event.getDescription());
        item.setPrice(event.getPrice());
        item.setActive(Boolean.TRUE.equals(event.getActive()));
        item.setImageUrl(event.getImageUrl());
        item.setImagePublicId(event.getImagePublicId());
        item.setPreparationTime(event.getPreparationTime());
        item.setRecipe(event.getRecipe());
        item.setIngredients(event.getIngredients() != null ? new ArrayList<>(event.getIngredients()) : new ArrayList<>());
        return item;
    }
    
    /**
     * Updates MenuItem entity from MenuItemUpdatedEvent
     */
    public void updateMenuItemFromEvent(MenuItem item, MenuItemUpdatedEvent event, Category category) {
        item.setName(event.getName());
        item.setCategory(category);
        item.setDescription(event.getDescription());
        item.setPrice(event.getPrice());
        item.setImageUrl(event.getImageUrl());
        item.setImagePublicId(event.getImagePublicId());
        item.setPreparationTime(event.getPreparationTime());
        item.setRecipe(event.getRecipe());
        
        // Update ingredients using entity methods to ensure business logic is applied
        if (item.getIngredients() != null) {
            item.getIngredients().clear();
        }
        if (event.getIngredients() != null) {
            for (String ingredientId : event.getIngredients()) {
                item.addIngredient(ingredientId);
            }
        }
    }
    
    /**
     * Maps CategoryCreatedEvent to Category entity
     */
    public Category toCategoryEntity(CategoryCreatedEvent event) {
        Category category = new Category();
        category.setCategoryId(event.getCategoryId());
        category.setName(event.getName());
        category.setDescription(event.getDescription());
        category.setType(event.getType());
        category.setActive(Boolean.TRUE.equals(event.getActive()));
        return category;
    }
    
    /**
     * Updates Category entity from CategoryUpdatedEvent
     */
    public void updateCategoryFromEvent(Category category, CategoryUpdatedEvent event) {
        category.setName(event.getName());
        category.setDescription(event.getDescription());
        category.setType(event.getType());
    }
    
    /**
     * Maps ComboCreatedEvent to Combo entity
     */
    public Combo toComboEntity(ComboCreatedEvent event) {
        Combo combo = new Combo();
        combo.setComboId(event.getComboId());
        combo.setName(event.getName());
        combo.setDescription(event.getDescription());
        combo.setPrice(event.getPrice());
        combo.setDiscount(event.getDiscount());
        combo.setActive(Boolean.TRUE.equals(event.getActive()));
        combo.setMenuItemIds(event.getMenuItemIds() != null ? new ArrayList<>(event.getMenuItemIds()) : new ArrayList<>());
        return combo;
    }
    
    /**
     * Updates Combo entity from ComboUpdatedEvent
     */
    public void updateComboFromEvent(Combo combo, ComboUpdatedEvent event) {
        combo.setName(event.getName());
        combo.setDescription(event.getDescription());
        combo.setPrice(event.getPrice());
        combo.setDiscount(event.getDiscount());
        
        // Update menu items using entity methods to ensure business logic is applied
        if (combo.getMenuItemIds() != null) {
            combo.getMenuItemIds().clear();
        }
        if (event.getMenuItemIds() != null) {
            for (String menuItemId : event.getMenuItemIds()) {
                combo.addMenuItem(menuItemId);
            }
        }
    }
}

