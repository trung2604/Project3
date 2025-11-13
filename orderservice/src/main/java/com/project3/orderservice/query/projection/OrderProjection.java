package com.project3.orderservice.query.projection;

import com.project3.orderservice.command.dto.OrderItemDTO;
import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.entity.OrderItem;
import com.project3.orderservice.command.entity.OrderItemRepository;
import com.project3.orderservice.command.entity.OrderRespository;
import com.project3.orderservice.query.dto.OrderResponse;
import com.project3.orderservice.query.queries.GetAllOrderQuery;
import com.project3.orderservice.query.queries.GetOrderByIdQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderProjection {

    @Autowired
    private OrderRespository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;

    @QueryHandler
    public List<OrderResponse> getAllOrder(GetAllOrderQuery query) {
        List<Order> orders;
        
        if (query.getStatus() != null && query.getStartDate() != null && query.getEndDate() != null) {
            orders = orderRepository.findByStatusAndDateRange(
                query.getStatus(), query.getStartDate(), query.getEndDate());
        } else if (query.getStartDate() != null && query.getEndDate() != null) {
            orders = orderRepository.findByOrderDateBetween(query.getStartDate(), query.getEndDate());
        } else if (query.getStatus() != null) {
            orders = orderRepository.findByOrderStatus(query.getStatus());
        } else if (query.getType() != null) {
            orders = orderRepository.findByOrderType(query.getType());
        } else if (query.getCustomerId() != null) {
            orders = orderRepository.findByCustomerId(query.getCustomerId());
        } else {
            orders = orderRepository.findAll();
        }
        
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @QueryHandler
    public OrderResponse getOrderById(GetOrderByIdQuery query) {
        Order order = orderRepository.findById(query.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + query.getOrderId()));
        return mapToOrderResponse(order);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setOrderType(order.getOrderType());
        response.setOrderStatus(order.getOrderStatus());
        response.setSubtotal(order.getSubtotal());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setDiscountPercentage(order.getDiscountPercentage());
        response.setVatAmount(order.getVatAmount());
        response.setVatPercentage(order.getVatPercentage());
        response.setTotalAmount(order.getTotalAmount());
        response.setOrderDate(order.getOrderDate());
        response.setCookingStartTime(order.getCookingStartTime());
        response.setReadyTime(order.getReadyTime());
        response.setCompletedTime(order.getCompletedTime());
        response.setCancelledTime(order.getCancelledTime());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setTableNumber(order.getTableNumber());
        response.setNotes(order.getNotes());
        response.setCreatedBy(order.getCreatedBy());
        response.setCancellationReason(order.getCancellationReason());
        
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
        List<OrderItemDTO> orderItemDTOs = orderItems.stream()
                .map(item -> {
                    OrderItemDTO dto = new OrderItemDTO();
                    dto.setMenuItemId(item.getMenuItemId());
                    dto.setName(item.getName());
                    dto.setQuantity(item.getQuantity());
                    dto.setUnitPrice(item.getUnitPrice());
                    dto.setSubtotal(item.getSubtotal());
                    dto.setNotes(item.getNotes());
                    return dto;
                })
                .collect(Collectors.toList());
        response.setOrderItems(orderItemDTOs);
        
        return response;
    }
}
