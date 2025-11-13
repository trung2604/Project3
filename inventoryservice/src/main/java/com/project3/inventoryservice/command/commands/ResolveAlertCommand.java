package com.project3.inventoryservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResolveAlertCommand {
    @TargetAggregateIdentifier
    private String alertId;
    private String resolvedBy; // User ID or username
}

