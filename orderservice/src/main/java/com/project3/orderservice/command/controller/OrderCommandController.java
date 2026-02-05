package com.project3.orderservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.orderservice.command.commands.*;
import com.project3.orderservice.command.entity.Order;
import com.project3.orderservice.command.enums.OrderStatus;
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
public class OrderCommandController extends BaseOrderController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping("/create")
    public ResponseEntity<ApiResponseDTO<String>> createOrder(
            @RequestBody CreateOrderCommand command,
            HttpServletRequest request) {
        try {
            String currentUserId = getCurrentUserId(request);
            command.setCreatedBy(currentUserId);
            
            if (command.getCustomerId() == null || command.getCustomerId().isEmpty()) {
                command.setCustomerId(currentUserId);
            }
            
            if (command.getOrderId() == null || command.getOrderId().isEmpty()) {
                command.setOrderId(UUID.randomUUID().toString());
            }
            
            log.info("Creating order: orderId={}, customerId={}, createdBy={}, currentUserId={}", 
                    command.getOrderId(), command.getCustomerId(), command.getCreatedBy(), currentUserId);
            
            String result = commandGateway.sendAndWait(command);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.created(result, "Order created successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Invalid order data: {}", e.getMessage());
            return badRequest(e.getMessage());
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            return badRequest("Failed to create order: " + e.getMessage());
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
            var currentUser = validateUser(request);
            if (currentUser == null) {
                return unauthorized("User not found");
            }
            
            if (!isStaffOrAbove(currentUser)) {
                return forbidden("Only staff and above can update order status");
            }
            
            String currentUserId = getCurrentUserId(request);
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
            return badRequest(e.getMessage());
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return badRequest("Failed to update order status: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponseDTO<String>> cancelOrder(
            @PathVariable String orderId,
            @RequestBody CancelOrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            var currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("User not found");
            }
            
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                return notFound("Order not found");
            }
            
            // Use entity method to check if order can be cancelled
            if (!order.canBeCancelled()) {
                return badRequest("Order cannot be cancelled. Current status: " + order.getOrderStatus());
            }
            
            String currentUserId = getCurrentUserId(httpRequest);
            if (!canModifyOrder(order, currentUserId, currentUser)) {
                return forbidden("You can only cancel your own orders");
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
            return badRequest(e.getMessage());
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error cancelling order: {}", e.getMessage(), e);
            return badRequest("Failed to cancel order: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/split-bill")
    public ResponseEntity<ApiResponseDTO<String>> splitBill(
            @PathVariable String orderId,
            @RequestBody SplitBillRequest request,
            HttpServletRequest httpRequest) {
        try {
            var currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("User not found");
            }
            
            if (!isStaffOrAbove(currentUser)) {
                return forbidden("Only staff and above can split bill");
            }
            
            String currentUserId = getCurrentUserId(httpRequest);
            SplitBillCommand command = new SplitBillCommand();
            command.setOriginalOrderId(orderId);
            command.setNewOrderIds(request.getNewOrderIds());
            command.setSplitItems(request.getSplitItems());
            command.setSplitBy(request.getSplitBy() != null ? request.getSplitBy() : currentUserId);
            
            String result = commandGateway.sendAndWait(command);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Bill split successfully"));
        } catch (IllegalStateException e) {
            log.error("Cannot split bill: {}", e.getMessage());
            return badRequest(e.getMessage());
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error splitting bill: {}", e.getMessage(), e);
            return badRequest("Failed to split bill: " + e.getMessage());
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
