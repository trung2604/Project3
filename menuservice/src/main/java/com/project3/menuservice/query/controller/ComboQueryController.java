package com.project3.menuservice.query.controller;

import com.project3.menuservice.query.dto.ComboResponse;
import com.project3.menuservice.query.queries.GetAllCombosQuery;
import com.project3.menuservice.query.queries.GetComboByIdQuery;
import com.project3.menuservice.query.queries.GetCombosByMenuItemQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant/combo")
public class ComboQueryController {

    @Autowired
    private QueryGateway queryGateway;

    @GetMapping
    public List<ComboResponse> getAll() {
        GetAllCombosQuery query = new GetAllCombosQuery();
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(ComboResponse.class)).join();
    }

    @GetMapping("/{id}")
    public ComboResponse getById(@PathVariable("id") String id) {
        GetComboByIdQuery query = new GetComboByIdQuery(id);
        return queryGateway.query(query, ResponseTypes.instanceOf(ComboResponse.class)).join();
    }

    @GetMapping("/menu-item/{menuItemId}")
    public List<ComboResponse> getByMenuItem(@PathVariable("menuItemId") String menuItemId) {
        GetCombosByMenuItemQuery query = new GetCombosByMenuItemQuery(menuItemId);
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(ComboResponse.class)).join();
    }
}
