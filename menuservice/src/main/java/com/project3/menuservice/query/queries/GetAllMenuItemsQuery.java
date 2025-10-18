package com.project3.menuservice.query.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllMenuItemsQuery {
    private String categoryId; // Changed from category to categoryId
    private Boolean active;
    private Double minPrice;
    private Double maxPrice;
    private Integer page;
    private Integer size;
}


