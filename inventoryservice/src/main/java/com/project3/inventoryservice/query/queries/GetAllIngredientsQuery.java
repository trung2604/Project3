package com.project3.inventoryservice.query.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllIngredientsQuery {
    private String category;
    private Boolean active;
    private Double minStock;
    private Double maxStock;
    private int page;
    private int size;
}
