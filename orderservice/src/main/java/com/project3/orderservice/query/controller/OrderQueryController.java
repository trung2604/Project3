package com.project3.orderservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.commonservice.security.SecurityUtils;
import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.enums.OrderType;
import com.project3.orderservice.command.service.UserInfoService;
import com.project3.orderservice.query.dto.OrderResponse;
import com.project3.orderservice.query.queries.GetAllOrderQuery;
import com.project3.orderservice.query.queries.GetOrderByIdQuery;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/restaurant/order")
@Slf4j
public class OrderQueryController {

    @Autowired
    private QueryGateway queryGateway;
    
    @Autowired
    private UserInfoService userInfoService;

    @GetMapping()
    public ResponseEntity<ApiResponseDTO<List<OrderResponse>>> getAllOrder(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderType type,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpServletRequest request) {
        try {
            String currentUserId;
            try {
                currentUserId = SecurityUtils.getUserIdFromHeader(request);
                log.debug("Retrieved userId from header: {}", currentUserId);
            } catch (org.springframework.web.server.ResponseStatusException e) {
                log.error("Missing X-User-Id header in request: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseDTO.error("Missing user identification. Please login again.", 401));
            }
            
            if (currentUserId == null || currentUserId.isEmpty()) {
                log.error("Empty userId from header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseDTO.error("Invalid user identification. Please login again.", 401));
            }
            
            var currentUser = userInfoService.getUserInfo(currentUserId);
            
            // If user not found in cache/service, still allow access but assume customer role
            // This prevents 401 errors when user service is temporarily unavailable
            boolean isStaffOrAbove = false;
            if (currentUser != null) {
                isStaffOrAbove = userInfoService.isStaffOrAbove(currentUser);
                log.debug("User {} has staff role: {}", currentUserId, isStaffOrAbove);
            } else {
                log.warn("User not found in cache/service for userId: {}, assuming customer role", currentUserId);
            }
            
            // For non-staff users, restrict to their own orders
            if (!isStaffOrAbove) {
                if (customerId == null) {
                    customerId = currentUserId;
                } else if (!customerId.equals(currentUserId)) {
                    log.warn("Non-staff user {} attempted to view orders for customer {}", currentUserId, customerId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponseDTO.error("You can only view your own orders", 403));
                }
            }
            
            GetAllOrderQuery query = new GetAllOrderQuery(status, type, customerId, startDate, endDate);
            List<OrderResponse> orders = queryGateway.query(query, ResponseTypes.multipleInstancesOf(OrderResponse.class)).join();
            
            // Additional filtering for non-staff users
            if (!isStaffOrAbove) {
                orders = orders.stream()
                        .filter(order -> order.getCustomerId() != null && order.getCustomerId().equals(currentUserId))
                        .collect(Collectors.toList());
            }
            
            return ResponseEntity.ok(ApiResponseDTO.success(orders, "Orders retrieved successfully"));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            log.error("ResponseStatusException in getAllOrder: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error retrieving orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve orders: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponseDTO<OrderResponse>> getOrderById(
            @PathVariable String orderId,
            HttpServletRequest request) {
        try {
            String currentUserId = SecurityUtils.getUserIdFromHeader(request);
            var currentUser = userInfoService.getUserInfo(currentUserId);
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseDTO.error("User not found", 401));
            }
            
            GetOrderByIdQuery query = new GetOrderByIdQuery(orderId);
            OrderResponse order = queryGateway.query(query, ResponseTypes.instanceOf(OrderResponse.class)).join();
            
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.notFound("Order not found with id: " + orderId));
            }
            
            boolean isOwner = order.getCustomerId().equals(currentUserId);
            boolean isStaffOrAbove = userInfoService.isStaffOrAbove(currentUser);
            
            if (!isOwner && !isStaffOrAbove) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponseDTO.error("You can only view your own orders", 403));
            }
            
            return ResponseEntity.ok(ApiResponseDTO.success(order, "Order retrieved successfully"));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error retrieving order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound("Order not found with id: " + orderId));
        }
    }
}
