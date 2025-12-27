package com.project3.menuservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id
    private String menuItemId;
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    private String description;
    private Double price;
    private Boolean active;
    private String imageUrl;
    private String imagePublicId; // Cloudinary public ID for deletion
    private Integer preparationTime;
    private String recipe; // Công thức món ăn

    // Legacy: Simple list of ingredient IDs (for backward compatibility)
    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "ingredient_id")
    @CollectionTable(name = "menu_item_ingredients_legacy", joinColumns = @JoinColumn(name = "menu_item_id"))
    private List<String> ingredients = new ArrayList<>();
    
    // New: Detailed ingredient information with quantities
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MenuItemIngredient> menuItemIngredients = new ArrayList<>();
    
    /**
     * Business logic: Checks if menu item is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }
    
    /**
     * Business logic: Activates the menu item
     */
    public void activate() {
        this.active = true;
    }
    
    /**
     * Business logic: Deactivates the menu item
     */
    public void deactivate() {
        this.active = false;
    }
    
    /**
     * Business logic: Toggles active status
     */
    public void toggleActive() {
        this.active = !Boolean.TRUE.equals(this.active);
    }
    
    /**
     * Business logic: Checks if menu item has ingredients
     */
    public boolean hasIngredients() {
        return this.ingredients != null && !this.ingredients.isEmpty();
    }
    
    /**
     * Business logic: Adds an ingredient to the menu item
     */
    public void addIngredient(String ingredientId) {
        if (ingredientId == null || ingredientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingredient ID cannot be null or empty");
        }
        if (this.ingredients == null) {
            this.ingredients = new ArrayList<>();
        }
        if (!this.ingredients.contains(ingredientId)) {
            this.ingredients.add(ingredientId);
        }
    }
    
    /**
     * Business logic: Removes an ingredient from the menu item
     */
    public void removeIngredient(String ingredientId) {
        if (this.ingredients != null) {
            this.ingredients.remove(ingredientId);
        }
    }
}


