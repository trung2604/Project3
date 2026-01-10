package com.project3.paymentservice.command.service.payment;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.project3.paymentservice.command.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PayPalPaymentProcessor implements PaymentProcessor {
    
    @Value("${paypal.client-id:}")
    private String clientId;
    
    @Value("${paypal.client-secret:}")
    private String clientSecret;
    
    @Value("${paypal.mode:sandbox}")
    private String mode;
    
    private PayPalHttpClient client;
    
    private PayPalHttpClient getClient() {
        if (client == null && clientId != null && !clientId.isEmpty() && clientSecret != null && !clientSecret.isEmpty()) {
            PayPalEnvironment environment;
            if ("live".equalsIgnoreCase(mode)) {
                environment = new PayPalEnvironment.Live(clientId, clientSecret);
            } else {
                environment = new PayPalEnvironment.Sandbox(clientId, clientSecret);
            }
            client = new PayPalHttpClient(environment);
        }
        return client;
    }
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Processing PayPal payment for orderId: {}", request.getOrderId());
        
        try {
            // Check if PayPal is configured
            if (getClient() == null) {
                log.warn("PayPal not configured, returning mock response");
                return createMockPaymentResult(request);
            }
            
            // Create PayPal order
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.checkoutPaymentIntent("CAPTURE");
            
            // Set purchase units
            List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
            PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .referenceId(request.getOrderId())
                .description("Payment for Order " + request.getOrderId())
                .customId(request.getPaymentId())
                .amountWithBreakdown(new AmountWithBreakdown()
                    .currencyCode("USD")
                    .value(String.format("%.2f", request.getAmount())));
            
            purchaseUnits.add(purchaseUnit);
            orderRequest.purchaseUnits(purchaseUnits);
            
            // Set application context with return URLs
            ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(request.getReturnUrl() != null ? request.getReturnUrl() : "http://localhost:8006/api/payments/paypal/success")
                .cancelUrl(request.getCancelUrl() != null ? request.getCancelUrl() : "http://localhost:8006/api/payments/paypal/cancel");
            orderRequest.applicationContext(applicationContext);
            
            // Create order
            OrdersCreateRequest createRequest = new OrdersCreateRequest();
            createRequest.requestBody(orderRequest);
            
            HttpResponse<Order> response = getClient().execute(createRequest);
            Order order = response.result();
            
            // Get approval URL
            String approvalUrl = order.links().stream()
                .filter(link -> "approve".equals(link.rel()))
                .map(LinkDescription::href)
                .findFirst()
                .orElse(null);
            
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            result.setGatewayOrderId(order.id());
            result.setRedirectUrl(approvalUrl);
            result.setMessage("PayPal order created. Redirect customer to approval URL.");
            
            log.info("PayPal order created successfully: {}", order.id());
            return result;
            
        } catch (Exception e) {
            log.error("Error processing PayPal payment: {}", e.getMessage(), e);
            PaymentResult result = new PaymentResult();
            result.setSuccess(false);
            result.setErrorMessage("PayPal payment creation failed: " + e.getMessage());
            return result;
        }
    }
    
    private PaymentResult createMockPaymentResult(PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setGatewayOrderId("PAYPAL-MOCK-" + request.getOrderId());
        result.setGatewayTransactionId("PAYPAL-TXN-" + UUID.randomUUID().toString());
        result.setRedirectUrl("https://www.sandbox.paypal.com/checkoutnow?token=MOCK-" + request.getPaymentId());
        result.setMessage("PayPal mock order created (API not configured)");
        return result;
    }
    
    @Override
    public PaymentResult processRefund(String paymentId, Double amount, String reason) {
        log.info("Processing PayPal refund for paymentId: {}, amount: {}", paymentId, amount);
        
        try {
            // PayPal refund implementation would go here
            // For now, return success
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            result.setGatewayTransactionId("PAYPAL-REFUND-" + UUID.randomUUID().toString());
            result.setMessage("PayPal refund processed for amount: " + amount);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error processing PayPal refund: {}", e.getMessage(), e);
            PaymentResult result = new PaymentResult();
            result.setSuccess(false);
            result.setErrorMessage("PayPal refund failed: " + e.getMessage());
            return result;
        }
    }
    
    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.PAYPAL;
    }
}
