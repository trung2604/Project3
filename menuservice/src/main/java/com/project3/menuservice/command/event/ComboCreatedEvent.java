package com.project3.menuservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComboCreatedEvent {
    private String comboId;
    private String name;
    private String description;
    private Double price;
    private Double discount;
    private List<String> menuItemIds;
    private Boolean active;
}
