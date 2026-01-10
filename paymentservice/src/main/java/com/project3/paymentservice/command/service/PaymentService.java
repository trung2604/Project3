package com.project3.paymentservice.command.service;

import com.project3.paymentservice.command.commands.CreatePaymentCommand;
import com.project3.paymentservice.command.commands.ProcessPaymentCommand;
import com.project3.paymentservice.command.commands.RefundPaymentCommand;
import com.project3.paymentservice.command.enums.PaymentMethod;
import com.project3.paymentservice.command.service.payment.PaymentProcessor;
import com.project3.paymentservice.command.service.payment.PaymentRequest;
import com.project3.paymentservice.command.service.payment.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private List<PaymentProcessor> paymentProcessors;
    
    public String createPayment(CreatePaymentCommand command) {
        try {
            // Generate payment ID if not provided
            if (command.getPaymentId() == null || command.getPaymentId().isEmpty()) {
                command.setPaymentId(UUID.randomUUID().toString());
            }
            
            // Send command to aggregate
            commandGateway.sendAndWait(command);
            log.info("Payment created: {}", command.getPaymentId());
            
            return command.getPaymentId();
            
        } catch (Exception e) {
            log.error("Error creating payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }
    
    public PaymentResult processPayment(String paymentId, PaymentRequest request) {
        try {
            // Find appropriate processor
            PaymentProcessor processor = paymentProcessors.stream()
                .filter(p -> p.supports(request.getPaymentMethod()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No processor found for method: " + request.getPaymentMethod()));
            
            // Process payment
            PaymentResult result = processor.processPayment(request);
            
            if (result.isSuccess()) {
                // Send process command toaggregate
                ProcessPaymentCommand command = new ProcessPaymentCommand();
                command.setPaymentId(paymentId);
                command.setGatewayOrderId(result.getGatewayOrderId());
                command.setGatewayTransactionId(result.getGatewayTransactionId());
                
                commandGateway.sendAndWait(command);
                log.info("Payment processed successfully: {}", paymentId);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage(), e);
            PaymentResult result = new PaymentResult();
            result.setSuccess(false);
            result.setErrorMessage("Payment processing failed: " + e.getMessage());
            return result;
        }
    }
    
    public PaymentResult refundPayment(String paymentId, Double refundAmount, String reason, String requestedBy) {
        try {
            // Send refund command to aggregate
            RefundPaymentCommand command = new RefundPaymentCommand();
            command.setPaymentId(paymentId);
            command.setRefundAmount(refundAmount);
            command.setReason(reason);
            command.setRequestedBy(requestedBy);
            
            commandGateway.sendAndWait(command);
            log.info("Payment refunded successfully: {}", paymentId);
            
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            result.setMessage("Refund processed successfully");
            return result;
            
        } catch (Exception e) {
            log.error("Error refunding payment: {}", e.getMessage(), e);
            PaymentResult result = new PaymentResult();
            result.setSuccess(false);
            result.setErrorMessage("Refund failed: " + e.getMessage());
            return result;
        }
    }
    
    private PaymentProcessor getProcessor(PaymentMethod method) {
        return paymentProcessors.stream()
            .filter(p -> p.supports(method))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No processor found for method: " + method));
    }
}
