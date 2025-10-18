package com.project3.menuservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RemoveMenuItemFromComboCommand {
    @TargetAggregateIdentifier
    private String comboId;
    private String menuItemId;
}
