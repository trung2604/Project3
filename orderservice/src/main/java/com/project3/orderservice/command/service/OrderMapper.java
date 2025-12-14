package com.project3.orderservice.command.service;

import com.project3.orderservice.command.dto.OrderItemDTO;
import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.entity.OrderItem;
import com.project3.orderservice.command.event.CreateOrderEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting between Order entities and DTOs/Events
 * Encapsulates mapping logic for better cohesion
 */
@Component
public class OrderMapper {
    
    /**
     * Maps CreateOrderEvent to Order entity
     */
    public Order toEntity(CreateOrderEvent event) {
        Order order = new Order();
        order.setOrderId(event.getOrderId());
        order.setCustomerId(event.getCustomerId());
        order.setCustomerName(event.getCustomerName());
        order.setCustomerPhone(event.getCustomerPhone());
        order.setOrderType(event.getOrderType());
        order.setOrderStatus(event.getOrderStatus());
        order.setSubtotal(event.getSubtotal());
        order.setDiscountAmount(event.getDiscountAmount());
        order.setDiscountPercentage(event.getDiscountPercentage());
        order.setVatAmount(event.getVatAmount());
        order.setVatPercentage(event.getVatPercentage());
        order.setTotalAmount(event.getTotalAmount());
        order.setOrderDate(event.getOrderDate());
        order.setDeliveryAddress(event.getDeliveryAddress());
        order.setTableNumber(event.getTableNumber());
        order.setNotes(event.getNotes());
        order.setCreatedBy(event.getCreatedBy());
        return order;
    }
    
    /**
     * Maps OrderItemDTO list to OrderItem entity list
     */
    public List<OrderItem> toOrderItemEntities(String orderId, List<OrderItemDTO> itemDTOs) {
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDTO itemDTO : itemDTOs) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderItemId(UUID.randomUUID().toString());
            orderItem.setOrderId(orderId);
            orderItem.setMenuItemId(itemDTO.getMenuItemId());
            orderItem.setName(itemDTO.getName());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setUnitPrice(itemDTO.getUnitPrice());
            orderItem.setSubtotal(itemDTO.getSubtotal());
            orderItem.setNotes(itemDTO.getNotes());
            orderItems.add(orderItem);
        }
        return orderItems;
    }
}

