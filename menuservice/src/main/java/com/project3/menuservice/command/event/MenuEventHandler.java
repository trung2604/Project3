package com.project3.menuservice.command.event;

import com.project3.menuservice.command.entity.Category;
import com.project3.menuservice.command.entity.CategoryRepository;
import com.project3.menuservice.command.entity.Combo;
import com.project3.menuservice.command.entity.ComboRepository;
import com.project3.menuservice.command.entity.MenuItem;
import com.project3.menuservice.command.entity.MenuItemRepository;
import com.project3.menuservice.service.CloudinaryService;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

@Component
public class MenuEventHandler {

    @Autowired
    private MenuItemRepository menuItemRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ComboRepository comboRepository;
    
    @Autowired
    private CloudinaryService cloudinaryService;

    @EventHandler
    public void on(MenuItemCreatedEvent event) {
        if (menuItemRepository.existsById(event.getMenuItemId())) {
            return;
        }
        MenuItem item = new MenuItem();
        item.setMenuItemId(event.getMenuItemId());
        item.setName(event.getName());
        
        // Set category by ID
        Category category = categoryRepository.findById(event.getCategoryId()).orElse(null);
        item.setCategory(category);
        
        item.setDescription(event.getDescription());
        item.setPrice(event.getPrice());
        item.setActive(Boolean.TRUE.equals(event.getActive()));
        item.setImageUrl(event.getImageUrl());
        item.setImagePublicId(event.getImagePublicId());
        item.setPreparationTime(event.getPreparationTime());
        item.setRecipe(event.getRecipe());
        item.setIngredients(event.getIngredients() != null ? new ArrayList<>(event.getIngredients()) : new ArrayList<>());
        menuItemRepository.save(item);
    }

    @EventHandler
    public void on(MenuItemUpdatedEvent event) {
        Optional<MenuItem> optional = menuItemRepository.findById(event.getMenuItemId());
        if (optional.isEmpty()) return;
        MenuItem item = optional.get();
        item.setName(event.getName());
        
        // Update category by ID
        Category category = categoryRepository.findById(event.getCategoryId()).orElse(null);
        item.setCategory(category);
        
        item.setDescription(event.getDescription());
        item.setPrice(event.getPrice());
        item.setImageUrl(event.getImageUrl());
        item.setImagePublicId(event.getImagePublicId());
        item.setPreparationTime(event.getPreparationTime());
        item.setRecipe(event.getRecipe());
        item.setIngredients(event.getIngredients() != null ? new ArrayList<>(event.getIngredients()) : new ArrayList<>());
        menuItemRepository.save(item);
    }

    @EventHandler
    public void on(MenuItemActiveToggledEvent event) {
        menuItemRepository.findById(event.getMenuItemId()).ifPresent(item -> {
            item.setActive(event.isActive());
            menuItemRepository.save(item);
        });
    }

    @EventHandler
    public void on(MenuItemIngredientsAttachedEvent event) {
        menuItemRepository.findById(event.getMenuItemId()).ifPresent(item -> {
            item.setIngredients(event.getIngredients() != null ? new ArrayList<>(event.getIngredients()) : new ArrayList<>());
            menuItemRepository.save(item);
        });
    }

    @EventHandler
    public void on(MenuItemDeletedEvent event) {
        // Delete image from Cloudinary if exists
        menuItemRepository.findById(event.getMenuItemId()).ifPresent(item -> {
            if (item.getImagePublicId() != null && !item.getImagePublicId().isEmpty()) {
                cloudinaryService.deleteImage(item.getImagePublicId());
            }
        });
        menuItemRepository.deleteById(event.getMenuItemId());
    }

    @EventHandler
    public void on(MenuItemAutoToggledEvent event) {
        menuItemRepository.findById(event.getMenuItemId()).ifPresent(item -> {
            item.setActive(event.getActive());
            menuItemRepository.save(item);
        });
    }

    // Category Event Handlers
    @EventHandler
    public void on(CategoryCreatedEvent event) {
        if (categoryRepository.existsById(event.getCategoryId())) {
            return;
        }
        Category category = new Category();
        category.setCategoryId(event.getCategoryId());
        category.setName(event.getName());
        category.setDescription(event.getDescription());
        category.setType(event.getType());
        category.setActive(Boolean.TRUE.equals(event.getActive()));
        categoryRepository.save(category);
    }

    @EventHandler
    public void on(CategoryUpdatedEvent event) {
        Optional<Category> optional = categoryRepository.findById(event.getCategoryId());
        if (optional.isEmpty()) return;
        Category category = optional.get();
        category.setName(event.getName());
        category.setDescription(event.getDescription());
        category.setType(event.getType());
        categoryRepository.save(category);
    }

    @EventHandler
    public void on(CategoryActiveToggledEvent event) {
        categoryRepository.findById(event.getCategoryId()).ifPresent(category -> {
            category.setActive(event.getActive());
            categoryRepository.save(category);
        });
    }

    @EventHandler
    public void on(CategoryDeletedEvent event) {
        categoryRepository.deleteById(event.getCategoryId());
    }

    // Combo Event Handlers
    @EventHandler
    public void on(ComboCreatedEvent event) {
        if (comboRepository.existsById(event.getComboId())) {
            return;
        }
        Combo combo = new Combo();
        combo.setComboId(event.getComboId());
        combo.setName(event.getName());
        combo.setDescription(event.getDescription());
        combo.setPrice(event.getPrice());
        combo.setDiscount(event.getDiscount());
        combo.setActive(Boolean.TRUE.equals(event.getActive()));
        combo.setMenuItemIds(event.getMenuItemIds() != null ? new ArrayList<>(event.getMenuItemIds()) : new ArrayList<>());
        comboRepository.save(combo);
    }

    @EventHandler
    public void on(ComboUpdatedEvent event) {
        Optional<Combo> optional = comboRepository.findById(event.getComboId());
        if (optional.isEmpty()) return;
        Combo combo = optional.get();
        combo.setName(event.getName());
        combo.setDescription(event.getDescription());
        combo.setPrice(event.getPrice());
        combo.setDiscount(event.getDiscount());
        combo.setMenuItemIds(event.getMenuItemIds() != null ? new ArrayList<>(event.getMenuItemIds()) : new ArrayList<>());
        comboRepository.save(combo);
    }

    @EventHandler
    public void on(ComboActiveToggledEvent event) {
        comboRepository.findById(event.getComboId()).ifPresent(combo -> {
            combo.setActive(event.getActive());
            comboRepository.save(combo);
        });
    }

    @EventHandler
    public void on(MenuItemAddedToComboEvent event) {
        comboRepository.findById(event.getComboId()).ifPresent(combo -> {
            if (!combo.getMenuItemIds().contains(event.getMenuItemId())) {
                combo.getMenuItemIds().add(event.getMenuItemId());
                comboRepository.save(combo);
            }
        });
    }

    @EventHandler
    public void on(MenuItemRemovedFromComboEvent event) {
        comboRepository.findById(event.getComboId()).ifPresent(combo -> {
            combo.getMenuItemIds().remove(event.getMenuItemId());
            comboRepository.save(combo);
        });
    }

    @EventHandler
    public void on(ComboDeletedEvent event) {
        comboRepository.deleteById(event.getComboId());
    }
}


