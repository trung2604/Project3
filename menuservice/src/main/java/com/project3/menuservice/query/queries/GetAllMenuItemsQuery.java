package com.project3.menuservice.query.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllMenuItemsQuery {
    private String categoryId;
    private Boolean active;
    private Double minPrice;
    private Double maxPrice;
    private String search; // Search by name or description
    private String sortBy; // Sort field: name, price, preparationTime, createdAt
    private String sortDirection; // Sort direction: asc, desc
    private Integer page;
    private Integer size;
}


