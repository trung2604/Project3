package com.project3.menuservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryCreatedEvent {
    private String categoryId;
    private String name;
    private String description;
    private String type;
    private Boolean active;
}
