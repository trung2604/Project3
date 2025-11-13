package com.project3.orderservice.command.aggregate;

import com.project3.orderservice.command.commands.*;
import com.project3.orderservice.command.dto.OrderItemDTO;
import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.enums.OrderType;
import com.project3.orderservice.command.event.*;
import com.project3.orderservice.command.util.OrderCalculator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDateTime;
import java.util.List;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Aggregate
public class OrderAggregate {
    @AggregateIdentifier
    private String orderId;
    
    private String customerId;
    private String customerName;
    private String customerPhone;
    private OrderType orderType;
    private OrderStatus orderStatus;
    
    private List<OrderItemDTO> orderItems;
    
    private Double subtotal;
    private Double discountAmount;
    private Double discountPercentage;
    private Double vatAmount;
    private Double vatPercentage;
    private Double totalAmount;
    
    private LocalDateTime orderDate;
    private LocalDateTime cookingStartTime;
    private LocalDateTime readyTime;
    private LocalDateTime completedTime;
    private LocalDateTime cancelledTime;
    
    private String deliveryAddress;
    private String tableNumber;
    private String notes;
    private String createdBy;

    @CommandHandler
    public OrderAggregate(CreateOrderCommand command) {
        if (command.getOrderId() == null || command.getOrderId().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (command.getOrderItems() == null || command.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("Order items are required");
        }
        
        for (OrderItemDTO item : command.getOrderItems()) {
            OrderCalculator.calculateOrderItemSubtotal(item);
        }
        
        Double subtotal = OrderCalculator.calculateSubtotal(command.getOrderItems());
        Double discountAmount = OrderCalculator.calculateDiscountAmount(
            subtotal, command.getDiscountPercentage());
        Double vatAmount = OrderCalculator.calculateVATAmount(
            subtotal, discountAmount, command.getVatPercentage());
        Double totalAmount = OrderCalculator.calculateTotalAmount(
            subtotal, discountAmount, vatAmount);
        
        CreateOrderEvent event = new CreateOrderEvent();
        event.setOrderId(command.getOrderId());
        event.setCustomerId(command.getCustomerId());
        event.setCustomerName(command.getCustomerName());
        event.setCustomerPhone(command.getCustomerPhone());
        event.setOrderType(command.getOrderType());
        event.setOrderStatus(OrderStatus.PENDING);
        event.setOrderItems(command.getOrderItems());
        event.setSubtotal(subtotal);
        event.setDiscountAmount(discountAmount);
        event.setDiscountPercentage(command.getDiscountPercentage());
        event.setVatAmount(vatAmount);
        event.setVatPercentage(command.getVatPercentage() != null ? command.getVatPercentage() : 10.0);
        event.setTotalAmount(totalAmount);
        event.setOrderDate(LocalDateTime.now());
        event.setDeliveryAddress(command.getDeliveryAddress());
        event.setTableNumber(command.getTableNumber());
        event.setNotes(command.getNotes());
        event.setCreatedBy(command.getCreatedBy());
        
        apply(event);
    }

    @CommandHandler
    public void handle(UpdateOrderStatusCommand command) {
        if (this.orderStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update status of a cancelled order");
        }
        if (this.orderStatus == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update status of a completed order");
        }
        
        OrderStatus previousStatus = this.orderStatus;
        
        if (!isValidStatusTransition(previousStatus, command.getNewStatus())) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", 
                    previousStatus, command.getNewStatus()));
        }
        
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent();
        event.setOrderId(command.getOrderId());
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(command.getNewStatus());
        event.setUpdatedAt(LocalDateTime.now());
        event.setUpdatedBy(command.getUpdatedBy());
        event.setNotes(command.getNotes());
        
        if (command.getNewStatus() == OrderStatus.COOKING) {
            event.setNotes(event.getNotes() != null ? event.getNotes() : "Order started cooking");
        } else if (command.getNewStatus() == OrderStatus.READY) {
            event.setNotes(event.getNotes() != null ? event.getNotes() : "Order is ready");
        } else if (command.getNewStatus() == OrderStatus.DELIVERING) {
            event.setNotes(event.getNotes() != null ? event.getNotes() : "Order is being delivered");
        } else if (command.getNewStatus() == OrderStatus.COMPLETED) {
            event.setNotes(event.getNotes() != null ? event.getNotes() : "Order completed");
        }
        
        apply(event);
        
        if (command.getNewStatus() == OrderStatus.COOKING) {
            requestInventoryDeduction();
        }
    }

    @CommandHandler
    public void handle(CancelOrderCommand command) {
        if (this.orderStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        if (this.orderStatus == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order");
        }
        
        if (!command.getAllowCancellation()) {
            throw new IllegalStateException("Cancellation is not allowed for this order");
        }
        
        if (this.orderStatus == OrderStatus.DELIVERING || this.orderStatus == OrderStatus.READY) {
            if (!isWithinCancellationWindow()) {
                throw new IllegalStateException("Cancellation window has expired");
            }
        }
        
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(command.getOrderId());
        event.setCancellationReason(command.getCancellationReason());
        event.setCancelledAt(LocalDateTime.now());
        event.setCancelledBy(command.getCancelledBy());
        
        apply(event);
    }

    @CommandHandler
    public void handle(SplitBillCommand command) {
        if (this.orderStatus == OrderStatus.COMPLETED || this.orderStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot split bill for completed or cancelled order");
        }
        
        BillSplitEvent event = new BillSplitEvent();
        event.setOriginalOrderId(command.getOriginalOrderId());
        event.setNewOrderIds(command.getNewOrderIds());
        event.setSplitAt(LocalDateTime.now());
        event.setSplitBy(command.getSplitBy());
        
        apply(event);
    }

    @EventSourcingHandler
    public void on(CreateOrderEvent event) {
        this.orderId = event.getOrderId();
        this.customerId = event.getCustomerId();
        this.customerName = event.getCustomerName();
        this.customerPhone = event.getCustomerPhone();
        this.orderType = event.getOrderType();
        this.orderStatus = event.getOrderStatus();
        this.orderItems = event.getOrderItems();
        this.subtotal = event.getSubtotal();
        this.discountAmount = event.getDiscountAmount();
        this.discountPercentage = event.getDiscountPercentage();
        this.vatAmount = event.getVatAmount();
        this.vatPercentage = event.getVatPercentage();
        this.totalAmount = event.getTotalAmount();
        this.orderDate = event.getOrderDate();
        this.deliveryAddress = event.getDeliveryAddress();
        this.tableNumber = event.getTableNumber();
        this.notes = event.getNotes();
        this.createdBy = event.getCreatedBy();
    }

    @EventSourcingHandler
    public void on(OrderStatusUpdatedEvent event) {
        this.orderStatus = event.getNewStatus();
        
        if (event.getNewStatus() == OrderStatus.COOKING && this.cookingStartTime == null) {
            this.cookingStartTime = event.getUpdatedAt();
        } else if (event.getNewStatus() == OrderStatus.READY) {
            this.readyTime = event.getUpdatedAt();
        } else if (event.getNewStatus() == OrderStatus.COMPLETED) {
            this.completedTime = event.getUpdatedAt();
        }
    }

    @EventSourcingHandler
    public void on(OrderCancelledEvent event) {
        this.orderStatus = OrderStatus.CANCELLED;
        this.cancelledTime = event.getCancelledAt();
    }

    @EventSourcingHandler
    public void on(BillSplitEvent event) {
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus newStatus) {
        if (current == null) return false;
        
        return switch (current) {
            case PENDING -> newStatus == OrderStatus.COOKING || newStatus == OrderStatus.CANCELLED;
            case COOKING -> newStatus == OrderStatus.READY || newStatus == OrderStatus.CANCELLED;
            case READY -> newStatus == OrderStatus.DELIVERING || newStatus == OrderStatus.CANCELLED;
            case DELIVERING -> newStatus == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    private boolean isWithinCancellationWindow() {
        if (this.orderDate == null) return false;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cancellationDeadline = this.orderDate.plusMinutes(30);
        return now.isBefore(cancellationDeadline);
    }

    private void requestInventoryDeduction() {
        InventoryDeductionRequestEvent event = new InventoryDeductionRequestEvent();
        event.setOrderId(this.orderId);
        event.setReason("Order started cooking - automatic stock deduction");
        
        apply(event);
    }
}

