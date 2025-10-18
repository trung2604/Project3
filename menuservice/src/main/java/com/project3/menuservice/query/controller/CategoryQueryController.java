package com.project3.menuservice.query.controller;

import com.project3.menuservice.query.dto.CategoryResponse;
import com.project3.menuservice.query.queries.GetAllCategoriesQuery;
import com.project3.menuservice.query.queries.GetCategoryByIdQuery;
import com.project3.menuservice.query.queries.GetCategoriesByTypeQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant/category")
public class CategoryQueryController {

    @Autowired
    private QueryGateway queryGateway;

    @GetMapping
    public List<CategoryResponse> getAll() {
        GetAllCategoriesQuery query = new GetAllCategoriesQuery();
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(CategoryResponse.class)).join();
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable("id") String id) {
        GetCategoryByIdQuery query = new GetCategoryByIdQuery(id);
        return queryGateway.query(query, ResponseTypes.instanceOf(CategoryResponse.class)).join();
    }

    @GetMapping("/type/{type}")
    public List<CategoryResponse> getByType(@PathVariable("type") String type) {
        GetCategoriesByTypeQuery query = new GetCategoriesByTypeQuery(type);
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(CategoryResponse.class)).join();
    }
}
