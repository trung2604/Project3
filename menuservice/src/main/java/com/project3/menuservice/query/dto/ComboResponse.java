package com.project3.menuservice.query.dto;

import com.project3.menuservice.command.entity.Combo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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

    public static ComboResponse fromEntity(Combo combo) {
        if (combo == null) return null;

        ComboResponse dto = new ComboResponse();
        dto.setComboId(combo.getComboId());
        dto.setName(combo.getName());
        dto.setDescription(combo.getDescription());
        dto.setPrice(combo.getPrice());
        dto.setDiscount(combo.getDiscount());
        dto.setActive(combo.getActive());
        try {
            if (combo.getMenuItemIds() != null) {
                dto.setMenuItemIds(new ArrayList<>(combo.getMenuItemIds()));
            } else {
                dto.setMenuItemIds(new ArrayList<>());
            }
        } catch (Exception e) {
            dto.setMenuItemIds(new ArrayList<>());
        }
        return dto;
    }
}
