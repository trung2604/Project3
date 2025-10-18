package com.project3.menuservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AutoToggleMenuItemCommand {
    @TargetAggregateIdentifier
    private String menuItemId;
    private String reason; // "INVENTORY_LOW", "INVENTORY_OUT", "INVENTORY_RESTOCKED"
    private String ingredientId;
}
