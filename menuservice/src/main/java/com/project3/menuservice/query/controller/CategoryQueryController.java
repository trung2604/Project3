package com.project3.menuservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.menuservice.query.dto.CategoryResponse;
import com.project3.menuservice.query.queries.GetAllCategoriesQuery;
import com.project3.menuservice.query.queries.GetCategoryByIdQuery;
import com.project3.menuservice.query.queries.GetCategoriesByTypeQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant/category")
@Slf4j
public class CategoryQueryController {

    @Autowired
    private QueryGateway queryGateway;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CategoryResponse>>> getAll() {
        try {
            GetAllCategoriesQuery query = new GetAllCategoriesQuery();
            List<CategoryResponse> categories = queryGateway.query(query, ResponseTypes.multipleInstancesOf(CategoryResponse.class)).join();
            log.info("Retrieved {} categories", categories != null ? categories.size() : 0);
            return ResponseEntity.ok(ApiResponseDTO.success(categories, "Categories retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving categories: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve categories: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CategoryResponse>> getById(@PathVariable("id") String id) {
        try {
            GetCategoryByIdQuery query = new GetCategoryByIdQuery(id);
            CategoryResponse category = queryGateway.query(query, ResponseTypes.instanceOf(CategoryResponse.class)).join();
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.notFound("Category not found with id: " + id));
            }
            return ResponseEntity.ok(ApiResponseDTO.success(category, "Category retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving category {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound("Category not found with id: " + id));
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponseDTO<List<CategoryResponse>>> getByType(@PathVariable("type") String type) {
        try {
            GetCategoriesByTypeQuery query = new GetCategoriesByTypeQuery(type);
            List<CategoryResponse> categories = queryGateway.query(query, ResponseTypes.multipleInstancesOf(CategoryResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(categories, "Categories retrieved successfully by type: " + type));
        } catch (Exception e) {
            log.error("Error retrieving categories by type {}: {}", type, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve categories by type: " + e.getMessage(), 500));
        }
    }
}
