package com.project3.orderservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.commonservice.security.SecurityUtils;
import com.project3.orderservice.command.commands.*;
import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.entity.OrderRespository;
import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.service.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/restaurant/order")
@Slf4j
public class OrderCommandController {

    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private OrderRespository orderRepository;
    
    @Autowired
    private UserInfoService userInfoService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponseDTO<String>> createOrder(
            @RequestBody CreateOrderCommand command,
            HttpServletRequest request) {
        try {
            String currentUserId = SecurityUtils.getUserIdFromHeader(request);
            command.setCreatedBy(currentUserId);
            
            if (command.getCustomerId() == null || command.getCustomerId().isEmpty()) {
                command.setCustomerId(currentUserId);
            }
            
            if (command.getOrderId() == null || command.getOrderId().isEmpty()) {
                command.setOrderId(UUID.randomUUID().toString());
            }
            
            String result = commandGateway.sendAndWait(command);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.created(result, "Order created successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Invalid order data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error(e.getMessage(), 400));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to create order: " + e.getMessage(), 400));
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponseDTO<String>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus newStatus,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) String notes,
            HttpServletRequest request) {
        try {
            String currentUserId = SecurityUtils.getUserIdFromHeader(request);
            var currentUser = userInfoService.getUserInfo(currentUserId);
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseDTO.error("User not found", 401));
            }
            
            if (!userInfoService.isStaffOrAbove(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponseDTO.error("Only staff and above can update order status", 403));
            }
            
            UpdateOrderStatusCommand command = new UpdateOrderStatusCommand();
            command.setOrderId(orderId);
            command.setNewStatus(newStatus);
            command.setUpdatedBy(updatedBy != null ? updatedBy : currentUserId);
            command.setNotes(notes);
            
            String result = commandGateway.sendAndWait(command);
            return ResponseEntity.ok(ApiResponseDTO.success(result, 
                "Order status updated to " + newStatus.name()));
        } catch (IllegalStateException e) {
            log.error("Invalid status transition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error(e.getMessage(), 400));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to update order status: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponseDTO<String>> cancelOrder(
            @PathVariable String orderId,
            @RequestBody CancelOrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            String currentUserId = SecurityUtils.getUserIdFromHeader(httpRequest);
            var currentUser = userInfoService.getUserInfo(currentUserId);
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseDTO.error("User not found", 401));
            }
            
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.error("Order not found", 404));
            }
            
            boolean isOwner = order.getCustomerId().equals(currentUserId);
            boolean isStaffOrAbove = userInfoService.isStaffOrAbove(currentUser);
            
            if (!isOwner && !isStaffOrAbove) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponseDTO.error("You can only cancel your own orders", 403));
            }
            
            CancelOrderCommand command = new CancelOrderCommand();
            command.setOrderId(orderId);
            command.setCancellationReason(request.getCancellationReason());
            command.setCancelledBy(request.getCancelledBy() != null ? request.getCancelledBy() : currentUserId);
            command.setAllowCancellation(request.getAllowCancellation() != null ? request.getAllowCancellation() : true);
            
            String result = commandGateway.sendAndWait(command);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Order cancelled successfully"));
        } catch (IllegalStateException e) {
            log.error("Cannot cancel order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error(e.getMessage(), 400));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error cancelling order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to cancel order: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/{orderId}/split-bill")
    public ResponseEntity<ApiResponseDTO<String>> splitBill(
            @PathVariable String orderId,
            @RequestBody SplitBillRequest request,
            HttpServletRequest httpRequest) {
        try {
            String currentUserId = SecurityUtils.getUserIdFromHeader(httpRequest);
            var currentUser = userInfoService.getUserInfo(currentUserId);
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseDTO.error("User not found", 401));
            }
            
            if (!userInfoService.isStaffOrAbove(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponseDTO.error("Only staff and above can split bill", 403));
            }
            
            SplitBillCommand command = new SplitBillCommand();
            command.setOriginalOrderId(orderId);
            command.setNewOrderIds(request.getNewOrderIds());
            command.setSplitItems(request.getSplitItems());
            command.setSplitBy(request.getSplitBy() != null ? request.getSplitBy() : currentUserId);
            
            String result = commandGateway.sendAndWait(command);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Bill split successfully"));
        } catch (IllegalStateException e) {
            log.error("Cannot split bill: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error(e.getMessage(), 400));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error splitting bill: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to split bill: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/{orderId}/start-cooking")
    public ResponseEntity<ApiResponseDTO<String>> startCooking(
            @PathVariable String orderId,
            @RequestParam(required = false) String updatedBy,
            HttpServletRequest request) {
        return updateOrderStatus(orderId, OrderStatus.COOKING, updatedBy, "Order started cooking", request);
    }

    @PostMapping("/{orderId}/mark-ready")
    public ResponseEntity<ApiResponseDTO<String>> markReady(
            @PathVariable String orderId,
            @RequestParam(required = false) String updatedBy,
            HttpServletRequest request) {
        return updateOrderStatus(orderId, OrderStatus.READY, updatedBy, "Order is ready", request);
    }

    @PostMapping("/{orderId}/start-delivering")
    public ResponseEntity<ApiResponseDTO<String>> startDelivering(
            @PathVariable String orderId,
            @RequestParam(required = false) String updatedBy,
            HttpServletRequest request) {
        return updateOrderStatus(orderId, OrderStatus.DELIVERING, updatedBy, "Order is being delivered", request);
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponseDTO<String>> completeOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String updatedBy,
            HttpServletRequest request) {
        return updateOrderStatus(orderId, OrderStatus.COMPLETED, updatedBy, "Order completed", request);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    static class CancelOrderRequest {
        private String cancellationReason;
        private String cancelledBy;
        private Boolean allowCancellation;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    static class SplitBillRequest {
        private java.util.List<String> newOrderIds;
        private java.util.List<java.util.List<com.project3.orderservice.command.dto.OrderItemDTO>> splitItems;
        private String splitBy;
    }
}
