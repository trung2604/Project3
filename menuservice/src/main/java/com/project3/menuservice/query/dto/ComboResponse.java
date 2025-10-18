package com.project3.menuservice.query.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComboResponse {
    private String comboId;
    private String name;
    private String description;
    private Double price;
    private Double discount;
    private Boolean active;
    private List<String> menuItemIds;
}
