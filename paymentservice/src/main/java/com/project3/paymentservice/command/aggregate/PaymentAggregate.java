package com.project3.paymentservice.command.aggregate;

import com.project3.paymentservice.command.commands.CreatePaymentCommand;
import com.project3.paymentservice.command.commands.ProcessPaymentCommand;
import com.project3.paymentservice.command.commands.RefundPaymentCommand;
import com.project3.paymentservice.command.enums.PaymentMethod;
import com.project3.paymentservice.command.enums.PaymentStatus;
import com.project3.paymentservice.command.enums.PaymentType;
import com.project3.paymentservice.command.events.PaymentCreatedEvent;
import com.project3.paymentservice.command.events.PaymentProcessedEvent;
import com.project3.paymentservice.command.events.PaymentRefundedEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDateTime;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Aggregate
public class PaymentAggregate {
    
    @AggregateIdentifier
    private String paymentId;
    
    private String orderId;
    private String customerId;
    private Double amount;
    private PaymentMethod paymentMethod;
    private PaymentType paymentType;
    private PaymentStatus status;
    private String transactionReference;
    private String gatewayOrderId;
    private String gatewayTransactionId;
    private Double refundedAmount = 0.0;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    
    @CommandHandler
    public PaymentAggregate(CreatePaymentCommand command) {
        // Validation
        if (command.getPaymentId() == null || command.getPaymentId().isEmpty()) {
            throw new IllegalArgumentException("Payment ID is required");
        }
        if (command.getOrderId() == null || command.getOrderId().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (command.getCustomerId() == null || command.getCustomerId().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (command.getAmount() == null || command.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (command.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        
        // Determine payment type based on method
        PaymentType type = command.getPaymentMethod() == PaymentMethod.CASH 
            ? PaymentType.POS 
            : PaymentType.ONLINE;
        
        PaymentCreatedEvent event = new PaymentCreatedEvent();
        event.setPaymentId(command.getPaymentId());
        event.setOrderId(command.getOrderId());
        event.setCustomerId(command.getCustomerId());
        event.setAmount(command.getAmount());
        event.setPaymentMethod(command.getPaymentMethod());
        event.setPaymentType(type);
        event.setCreatedAt(LocalDateTime.now());
        event.setIpAddress(command.getIpAddress());
        event.setUserAgent(command.getUserAgent());
        
        apply(event);
    }
    
    @EventSourcingHandler
    public void on(PaymentCreatedEvent event) {
        this.paymentId = event.getPaymentId();
        this.orderId = event.getOrderId();
        this.customerId = event.getCustomerId();
        this.amount = event.getAmount();
        this.paymentMethod = event.getPaymentMethod();
        this.paymentType = event.getPaymentType();
        this.status = PaymentStatus.PENDING;
        this.createdAt = event.getCreatedAt();
        this.refundedAmount = 0.0;
    }
    
    @CommandHandler
    public void handle(ProcessPaymentCommand command) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Payment can only be processed from PENDING or PROCESSING status. Current status: " + this.status);
        }
        
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setPaymentId(this.paymentId);
        event.setOrderId(this.orderId);
        event.setCustomerId(this.customerId);
        event.setAmount(this.amount);
        event.setStatus(PaymentStatus.SUCCESS);
        event.setTransactionReference(command.getPaymentId() + "-" + System.currentTimeMillis());
        event.setGatewayOrderId(command.getGatewayOrderId());
        event.setGatewayTransactionId(command.getGatewayTransactionId());
        event.setGatewayResponse("Payment processed successfully");
        event.setProcessedAt(LocalDateTime.now());
        event.setFailureReason(null);
        
        apply(event);
    }
    
    @EventSourcingHandler
    public void on(PaymentProcessedEvent event) {
        this.status = event.getStatus();
        this.transactionReference = event.getTransactionReference();
        this.gatewayOrderId = event.getGatewayOrderId();
        this.gatewayTransactionId = event.getGatewayTransactionId();
        this.processedAt = event.getProcessedAt();
    }
    
    @CommandHandler
    public void handle(RefundPaymentCommand command) {
        // Validation
        if (this.status != PaymentStatus.SUCCESS && this.status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Only successful or partially refunded payments can be refunded. Current status: " + this.status);
        }
        
        if (command.getRefundAmount() == null || command.getRefundAmount() <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than 0");
        }
        
        double totalRefundable = this.amount - this.refundedAmount;
        if (command.getRefundAmount() > totalRefundable) {
            throw new IllegalArgumentException("Refund amount (" + command.getRefundAmount() + 
                ") exceeds refundable amount (" + totalRefundable + ")");
        }
        
        // Determine new status
        double newRefundedAmount = this.refundedAmount + command.getRefundAmount();
        PaymentStatus newStatus = (newRefundedAmount >= this.amount) 
            ? PaymentStatus.REFUNDED 
            : PaymentStatus.PARTIALLY_REFUNDED;
        
        PaymentRefundedEvent event = new PaymentRefundedEvent();
        event.setPaymentId(this.paymentId);
        event.setOrderId(this.orderId);
        event.setCustomerId(this.customerId);
        event.setRefundAmount(command.getRefundAmount());
        event.setNewStatus(newStatus);
        event.setReason(command.getReason());
        event.setRequestedBy(command.getRequestedBy());
        event.setRefundedAt(LocalDateTime.now());
        event.setGatewayResponse("Refund processed successfully");
        
        apply(event);
    }
    
    @EventSourcingHandler
    public void on(PaymentRefundedEvent event) {
        this.status = event.getNewStatus();
        this.refundedAmount += event.getRefundAmount();
    }
}
