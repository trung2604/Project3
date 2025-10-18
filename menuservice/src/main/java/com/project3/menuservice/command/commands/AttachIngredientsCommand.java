package com.project3.menuservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachIngredientsCommand {
    @TargetAggregateIdentifier
    private String menuItemId;
    private List<String> ingredients;
}


