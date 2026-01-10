package com.project3.paymentservice.command.event;

import com.project3.paymentservice.command.entity.Payment;
import com.project3.paymentservice.command.entity.PaymentRepository;
import com.project3.paymentservice.command.events.PaymentCreatedEvent;
import com.project3.paymentservice.command.events.PaymentProcessedEvent;
import com.project3.paymentservice.command.events.PaymentRefundedEvent;
import com.project3.paymentservice.command.service.PaymentEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventHandler {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentEventPublisher eventPublisher;
    
    @EventHandler
    public void on(PaymentCreatedEvent event) {
        try {
            log.info("Handling PaymentCreatedEvent for paymentId: {}", event.getPaymentId());
            
            Payment payment = new Payment();
            payment.setPaymentId(event.getPaymentId());
            payment.setOrderId(event.getOrderId());
            payment.setCustomerId(event.getCustomerId());
            payment.setAmount(event.getAmount());
            payment.setPaymentMethod(event.getPaymentMethod());
            payment.setPaymentType(event.getPaymentType());
            payment.setStatus(com.project3.paymentservice.command.enums.PaymentStatus.PENDING);
            payment.setCreatedAt(event.getCreatedAt());
            payment.setIpAddress(event.getIpAddress());
            payment.setUserAgent(event.getUserAgent());
            payment.setRefundedAmount(0.0);
            
            paymentRepository.save(payment);
            log.info("Payment created and saved: {}", event.getPaymentId());
            
        } catch (Exception e) {
            log.error("Error handling PaymentCreatedEvent: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @EventHandler
    public void on(PaymentProcessedEvent event) {
        try {
            log.info("Handling PaymentProcessedEvent for paymentId: {}", event.getPaymentId());
            
            Payment payment = paymentRepository.findById(event.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + event.getPaymentId()));
            
            payment.setStatus(event.getStatus());
            payment.setTransactionReference(event.getTransactionReference());
            payment.setGatewayOrderId(event.getGatewayOrderId());
            payment.setGatewayTransactionId(event.getGatewayTransactionId());
            payment.setGatewayResponse(event.getGatewayResponse());
            payment.setProcessedAt(event.getProcessedAt());
            payment.setFailureReason(event.getFailureReason());
            
            paymentRepository.save(payment);
            log.info("Payment processed and saved: {}", event.getPaymentId());
            
            // Publish Kafka event to OrderService and LoyaltyService
            if (event.getStatus() == com.project3.paymentservice.command.enums.PaymentStatus.SUCCESS) {
                eventPublisher.publishPaymentCompleted(payment);
            } else {
                eventPublisher.publishPaymentFailed(payment);
            }
            
        } catch (Exception e) {
            log.error("Error handling PaymentProcessedEvent: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @EventHandler
    public void on(PaymentRefundedEvent event) {
        try {
            log.info("Handling PaymentRefundedEvent for paymentId: {}", event.getPaymentId());
            
            Payment payment = paymentRepository.findById(event.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + event.getPaymentId()));
            
            payment.setStatus(event.getNewStatus());
            payment.setRefundedAmount((payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0.0) + event.getRefundAmount());
            payment.setRefundedAt(event.getRefundedAt());
            payment.setNotes("Refund: " + event.getReason() + " by " + event.getRequestedBy());
            
            paymentRepository.save(payment);
            log.info("Payment refunded and saved: {}", event.getPaymentId());
            
            // Publish Kafka event to OrderService and LoyaltyService
            eventPublisher.publishPaymentRefunded(payment, event.getRefundAmount(), event.getReason());
            
        } catch (Exception e) {
            log.error("Error handling PaymentRefundedEvent: {}", e.getMessage(), e);
            throw e;
        }
    }
}
