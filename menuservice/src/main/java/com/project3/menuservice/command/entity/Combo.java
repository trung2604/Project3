package com.project3.menuservice.command.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "combos")
public class Combo {
    @Id
    private String comboId;
    private String name;
    private String description;
    private Double price;
    private Double discount;
    private Boolean active;

    @ElementCollection
    private List<String> menuItemIds = new ArrayList<>();
}
