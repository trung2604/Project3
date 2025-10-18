package com.project3.menuservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryActiveToggledEvent {
    private String categoryId;
    private Boolean active;
}
