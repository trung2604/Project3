package com.project3.orderservice.command.controller;

import com.project3.orderservice.command.commands.CreateOrderCommand;
import com.project3.orderservice.command.entity.Order;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("api/restaurant/order")
public class OrderCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping("/create")
    public String createOrder(@RequestBody CreateOrderCommand order) {
        return commandGateway.sendAndWait(order);
    }
}
