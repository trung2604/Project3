package com.project3.menuservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemAutoToggledEvent {
    private String menuItemId;
    private Boolean active;
    private String reason;
    private String ingredientId;
}
