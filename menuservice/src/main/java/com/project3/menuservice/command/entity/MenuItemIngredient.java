package com.project3.menuservice.command.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the relationship between MenuItem and Ingredient
 * with quantity and unit information
 */
@Entity
@Table(name = "menu_item_ingredients", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"menu_item_id", "ingredient_id"})
       })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemIngredient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;
    
    @Column(name = "ingredient_id", nullable = false)
    private String ingredientId;
    
    // Convenience method to get menuItemId
    public String getMenuItemId() {
        if (menuItem != null) {
            return menuItem.getMenuItemId();
        }
        return null;
    }
    
    // Convenience method to set menuItemId (creates MenuItem reference if needed)
    public void setMenuItemId(String menuItemId) {
        // This is a convenience setter, but we need menuItem to be set
        // This will be handled by setting menuItem directly
    }
    
    /**
     * Quantity needed per serving of the menu item
     * Example: 0.2 kg of onion per 1 serving of fried chicken
     */
    @Column(nullable = false)
    private Double quantity;
    
    /**
     * Unit of measurement (should match ingredient's unit)
     * Example: "kg", "liter", "piece"
     */
    @Column(nullable = false, length = 20)
    private String unit;
    
    /**
     * Optional notes about how this ingredient is used
     */
    @Column(length = 500)
    private String notes;
}

