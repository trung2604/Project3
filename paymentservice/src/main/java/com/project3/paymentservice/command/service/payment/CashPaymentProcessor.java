package com.project3.paymentservice.command.service.payment;

import com.project3.paymentservice.command.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class CashPaymentProcessor implements PaymentProcessor {
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Processing cash payment for orderId: {}", request.getOrderId());
        
        try {
            // Cash payments are instant and always successful in POS
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            result.setGatewayOrderId("CASH-" + request.getOrderId());
            result.setGatewayTransactionId("CASH-TXN-" + UUID.randomUUID().toString());
            result.setMessage("Cash payment received successfully");
            
            log.info("Cash payment successful for orderId: {}", request.getOrderId());
            return result;
            
        } catch (Exception e) {
            log.error("Error processing cash payment: {}", e.getMessage(), e);
            PaymentResult result = new PaymentResult();
            result.setSuccess(false);
            result.setErrorMessage("Cash payment processing failed: " + e.getMessage());
            return result;
        }
    }
    
    @Override
    public PaymentResult processRefund(String paymentId, Double amount, String reason) {
        log.info("Processing cash refund for paymentId: {}, amount: {}", paymentId, amount);
        
        // Cash refunds are manual - just record it
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setGatewayTransactionId("CASH-REFUND-" + UUID.randomUUID().toString());
        result.setMessage("Cash refund recorded. Amount: " + amount + ". Please process manual refund.");
        
        return result;
    }
    
    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.CASH;
    }
}
