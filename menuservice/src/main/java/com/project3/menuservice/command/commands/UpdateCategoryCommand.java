package com.project3.menuservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCategoryCommand {
    @TargetAggregateIdentifier
    private String categoryId;
    private String name;
    private String description;
    private String type;
}
