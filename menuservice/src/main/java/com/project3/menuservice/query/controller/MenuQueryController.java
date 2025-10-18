package com.project3.menuservice.query.controller;

import com.project3.menuservice.query.dto.MenuItemResponse;
import com.project3.menuservice.query.dto.PagedMenuItemResponse;
import com.project3.menuservice.query.queries.GetAllMenuItemsQuery;
import com.project3.menuservice.query.queries.GetMenuItemByIdQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant/menu")
public class MenuQueryController {

    @Autowired
    private QueryGateway queryGateway;

    @GetMapping("/items")
    public PagedMenuItemResponse getAll(@RequestParam(required = false) String categoryId,
                                       @RequestParam(required = false) Boolean active,
                                       @RequestParam(required = false) Double minPrice,
                                       @RequestParam(required = false) Double maxPrice,
                                       @RequestParam(defaultValue = "0") Integer page,
                                       @RequestParam(defaultValue = "20") Integer size) {
        GetAllMenuItemsQuery query = new GetAllMenuItemsQuery(categoryId, active, minPrice, maxPrice, page, size);
        return queryGateway.query(query, ResponseTypes.instanceOf(PagedMenuItemResponse.class)).join();
    }

    @GetMapping("/items/{id}")
    public MenuItemResponse getById(@PathVariable("id") String id) {
        GetMenuItemByIdQuery query = new GetMenuItemByIdQuery(id);
        return queryGateway.query(query, ResponseTypes.instanceOf(MenuItemResponse.class)).join();
    }
}


