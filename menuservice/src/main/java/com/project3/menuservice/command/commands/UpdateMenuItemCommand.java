package com.project3.menuservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMenuItemCommand {
    @TargetAggregateIdentifier
    private String menuItemId;
    private String name;
    private String categoryId; // Changed from category string to categoryId
    private String description;
    private Double price;
    private List<String> ingredients;
}


