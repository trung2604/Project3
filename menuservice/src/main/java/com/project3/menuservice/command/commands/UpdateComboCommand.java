package com.project3.menuservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateComboCommand {
    @TargetAggregateIdentifier
    private String comboId;
    private String name;
    private String description;
    private Double price;
    private Double discount;
    private List<String> menuItemIds;
}
