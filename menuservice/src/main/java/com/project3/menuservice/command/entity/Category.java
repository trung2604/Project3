package com.project3.menuservice.command.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category {
    @Id
    private String categoryId;
    private String name;
    private String description;
    private String type;
    private Boolean active;
    
    /**
     * Business logic: Checks if category is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }
    
    /**
     * Business logic: Activates the category
     */
    public void activate() {
        this.active = true;
    }
    
    /**
     * Business logic: Deactivates the category
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
}
