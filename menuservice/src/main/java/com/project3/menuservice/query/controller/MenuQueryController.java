package com.project3.menuservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.menuservice.query.dto.MenuItemResponse;
import com.project3.menuservice.query.dto.PagedMenuItemResponse;
import com.project3.menuservice.query.queries.GetAllMenuItemsQuery;
import com.project3.menuservice.query.queries.GetMenuItemByIdQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant/menu")
@Slf4j
public class MenuQueryController {

    @Autowired
    private QueryGateway queryGateway;

    @GetMapping("/items")
    public ResponseEntity<ApiResponseDTO<PagedMenuItemResponse>> getAll(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            GetAllMenuItemsQuery query = new GetAllMenuItemsQuery(
                categoryId, active, minPrice, maxPrice, search, sortBy, sortDirection, page, size
            );
            PagedMenuItemResponse response = queryGateway.query(query, ResponseTypes.instanceOf(PagedMenuItemResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Menu items retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving menu items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve menu items: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponseDTO<MenuItemResponse>> getById(@PathVariable("id") String id) {
        try {
            GetMenuItemByIdQuery query = new GetMenuItemByIdQuery(id);
            MenuItemResponse item = queryGateway.query(query, ResponseTypes.instanceOf(MenuItemResponse.class)).join();
            if (item == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.notFound("Menu item not found with id: " + id));
            }
            return ResponseEntity.ok(ApiResponseDTO.success(item, "Menu item retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving menu item {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound("Menu item not found with id: " + id));
        }
    }
}