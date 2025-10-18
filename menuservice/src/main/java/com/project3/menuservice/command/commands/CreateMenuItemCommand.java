package com.project3.menuservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateMenuItemCommand {
    @TargetAggregateIdentifier
    private String menuItemId;
    private String name;
    private String categoryId; // Changed from category string to categoryId
    private String description;
    private Double price;
    private Boolean active;
    private List<String> ingredients; // list of ingredient IDs or names
}


