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
    
    @Autowired
    private com.project3.menuservice.command.service.MenuMapper menuMapper;

    @EventHandler
    public void on(MenuItemCreatedEvent event) {
        if (menuItemRepository.existsById(event.getMenuItemId())) {
            return;
        }
        Category category = categoryRepository.findById(event.getCategoryId()).orElse(null);
        MenuItem item = menuMapper.toMenuItemEntity(event, category);
        menuItemRepository.save(item);
    }

    @EventHandler
    public void on(MenuItemUpdatedEvent event) {
        Optional<MenuItem> optional = menuItemRepository.findById(event.getMenuItemId());
        if (optional.isEmpty()) return;
        MenuItem item = optional.get();
        Category category = categoryRepository.findById(event.getCategoryId()).orElse(null);
        menuMapper.updateMenuItemFromEvent(item, event, category);
        menuItemRepository.save(item);
    }

    @EventHandler
    public void on(MenuItemActiveToggledEvent event) {
        menuItemRepository.findById(event.getMenuItemId()).ifPresent(item -> {
            if (event.isActive()) {
                item.activate();
            } else {
                item.deactivate();
            }
            menuItemRepository.save(item);
        });
    }

    @EventHandler
    public void on(MenuItemIngredientsAttachedEvent event) {
        menuItemRepository.findById(event.getMenuItemId()).ifPresent(item -> {
            // Clear existing ingredients and add new ones
            if (item.getIngredients() != null) {
                item.getIngredients().clear();
            }
            if (event.getIngredients() != null) {
                for (String ingredientId : event.getIngredients()) {
                    item.addIngredient(ingredientId);
                }
            }
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
            if (Boolean.TRUE.equals(event.getActive())) {
                item.activate();
            } else {
                item.deactivate();
            }
            menuItemRepository.save(item);
        });
    }

    // Category Event Handlers
    @EventHandler
    public void on(CategoryCreatedEvent event) {
        if (categoryRepository.existsById(event.getCategoryId())) {
            return;
        }
        Category category = menuMapper.toCategoryEntity(event);
        categoryRepository.save(category);
    }

    @EventHandler
    public void on(CategoryUpdatedEvent event) {
        Optional<Category> optional = categoryRepository.findById(event.getCategoryId());
        if (optional.isEmpty()) return;
        Category category = optional.get();
        menuMapper.updateCategoryFromEvent(category, event);
        categoryRepository.save(category);
    }

    @EventHandler
    public void on(CategoryActiveToggledEvent event) {
        categoryRepository.findById(event.getCategoryId()).ifPresent(category -> {
            if (event.getActive()) {
                category.activate();
            } else {
                category.deactivate();
            }
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
        Combo combo = menuMapper.toComboEntity(event);
        comboRepository.save(combo);
    }

    @EventHandler
    public void on(ComboUpdatedEvent event) {
        Optional<Combo> optional = comboRepository.findById(event.getComboId());
        if (optional.isEmpty()) return;
        Combo combo = optional.get();
        menuMapper.updateComboFromEvent(combo, event);
        comboRepository.save(combo);
    }

    @EventHandler
    public void on(ComboActiveToggledEvent event) {
        comboRepository.findById(event.getComboId()).ifPresent(combo -> {
            if (event.getActive()) {
                combo.activate();
            } else {
                combo.deactivate();
            }
            comboRepository.save(combo);
        });
    }

    @EventHandler
    public void on(MenuItemAddedToComboEvent event) {
        comboRepository.findById(event.getComboId()).ifPresent(combo -> {
            combo.addMenuItem(event.getMenuItemId());
            comboRepository.save(combo);
        });
    }

    @EventHandler
    public void on(MenuItemRemovedFromComboEvent event) {
        comboRepository.findById(event.getComboId()).ifPresent(combo -> {
            combo.removeMenuItem(event.getMenuItemId());
            comboRepository.save(combo);
        });
    }

    @EventHandler
    public void on(ComboDeletedEvent event) {
        comboRepository.deleteById(event.getComboId());
    }
}


