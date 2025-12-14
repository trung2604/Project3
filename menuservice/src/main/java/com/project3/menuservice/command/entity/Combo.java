package com.project3.menuservice.command.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "combos")
public class Combo {
    @Id
    private String comboId;
    private String name;
    private String description;
    private Double price;
    private Double discount;
    private Boolean active;

    @ElementCollection
    private List<String> menuItemIds = new ArrayList<>();
    
    /**
     * Business logic: Checks if combo is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }
    
    /**
     * Business logic: Activates the combo
     */
    public void activate() {
        this.active = true;
    }
    
    /**
     * Business logic: Deactivates the combo
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
     * Business logic: Adds a menu item to the combo
     */
    public void addMenuItem(String menuItemId) {
        if (menuItemId == null || menuItemId.trim().isEmpty()) {
            throw new IllegalArgumentException("MenuItem ID cannot be null or empty");
        }
        if (this.menuItemIds == null) {
            this.menuItemIds = new ArrayList<>();
        }
        if (!this.menuItemIds.contains(menuItemId)) {
            this.menuItemIds.add(menuItemId);
        }
    }
    
    /**
     * Business logic: Removes a menu item from the combo
     */
    public void removeMenuItem(String menuItemId) {
        if (this.menuItemIds != null) {
            this.menuItemIds.remove(menuItemId);
        }
    }
    
    /**
     * Business logic: Checks if combo has menu items
     */
    public boolean hasMenuItems() {
        return this.menuItemIds != null && !this.menuItemIds.isEmpty();
    }
}
