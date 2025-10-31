package com.project3.menuservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemCreatedEvent {
    private String menuItemId;
    private String name;
    private String categoryId; // Changed from category string to categoryId
    private String description;
    private Double price;
    private Boolean active;
    private String imageUrl;
    private String imagePublicId;
    private Integer preparationTime;
    private String recipe;
    private List<String> ingredients;
}


