package com.project3.orderservice.query.controller;

import com.project3.orderservice.query.dto.OrderRespone;
import com.project3.orderservice.query.queries.GetAllOrderQuery;
import com.project3.orderservice.query.queries.GetOrderByIdQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant/order")
public class OrderQueryController {

    @Autowired
    private QueryGateway queryGateway;

    @GetMapping()
    public List<OrderRespone> getAllOrder() {
        GetAllOrderQuery getAllOrderQuery = new GetAllOrderQuery();
        return queryGateway.query(getAllOrderQuery, ResponseTypes.multipleInstancesOf(OrderRespone.class)).join();
    }
    @GetMapping("/{orderId}")
    public OrderRespone getOrderById(@PathVariable String orderId) {
        GetOrderByIdQuery getOrderByIdQuery = new GetOrderByIdQuery(orderId);
        return queryGateway.query(getOrderById(orderId), ResponseTypes.instanceOf(OrderRespone.class)).join();
    }
}
