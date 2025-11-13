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
    private String search; // Search by name, description, or supplier name
    private String sortBy; // Sort field: name, currentStock, unitCost, expiryDate, createdAt
    private String sortDirection; // Sort direction: asc, desc
    private int page;
    private int size;
}
