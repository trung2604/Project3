package com.project3.orderservice.query.projection;

import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.entity.OrderRespository;
import com.project3.orderservice.query.dto.OrderRespone;
import com.project3.orderservice.query.queries.GetAllOrderQuery;
import com.project3.orderservice.query.queries.GetOrderByIdQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderProjection {

    @Autowired
    private OrderRespository orderRespository;

    @QueryHandler
    public List<OrderRespone> getAllOrder(GetAllOrderQuery getAllOrderQuery) {
        List<Order> orders = orderRespository.findAll();
        List<OrderRespone> orderRespones = new ArrayList<>();
        orders.forEach(order -> {
            OrderRespone orderRespone = new OrderRespone();
            BeanUtils.copyProperties(order, orderRespone);
            orderRespones.add(orderRespone);
        });
        return orderRespones;
    }

    @QueryHandler
    public OrderRespone getOrderId(GetOrderByIdQuery query) {
        Order order = orderRespository.findById(query.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));
        OrderRespone orderRespone = new OrderRespone();
        if(order != null) {
            BeanUtils.copyProperties(order, orderRespone);
        }
        return orderRespone;
    }
}
