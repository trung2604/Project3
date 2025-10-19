package com.project3.orderservice.command.event;

import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.entity.OrderRespository;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderEventHandler {
    @Autowired
    private OrderRespository orderRespository;

    @EventHandler
    public void on(CreateOrderEvent event) {
        if (orderRespository.existsById(event.getOrderID())) {
            return;
        }

        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setCustomerId(event.getCustomerId());
        order.setOrderStatus(event.getOrderStatus());
        order.setOrderAmount(event.getOrderAmount());
        order.setOrderDate(event.getOrderDate());
        order.setVat(event.getVat());

        orderRespository.save(order);
    }

}
