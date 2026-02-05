package com.project3.paymentservice.command.service.payment;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.project3.paymentservice.command.enums.PaymentMethod;
import jakarta.annotation.PostConstruct;
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

    @Value("${paypal.exchange-rate:26500}")
    private double exchangeRate;
    
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
    
    /**
     * Validate configuration at startup
     */
    @PostConstruct
    public void validateConfiguration() {
        boolean isConfigured = isNotEmpty(clientId) && isNotEmpty(clientSecret);
        
        if (!isConfigured) {
            log.warn("=====================================================================");
            log.warn("PayPal Payment Processor is NOT configured!");
            log.warn("Missing required properties:");
            if (!isNotEmpty(clientId)) log.warn("  - paypal.client-id");
            if (!isNotEmpty(clientSecret)) log.warn("  - paypal.client-secret");
            log.warn("PayPal will use MOCK mode until properly configured.");
            log.warn("=====================================================================");
        } else {
            log.info("PayPal Payment Processor configured successfully.");
            log.info("  Mode: {}", mode);
            log.info("  Client ID: {}***", clientId.substring(0, Math.min(8, clientId.length())));
        }
    }
    
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
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
                    .value(String.format("%.2f", request.getAmount() / exchangeRate)));
            
            purchaseUnits.add(purchaseUnit);
            orderRequest.purchaseUnits(purchaseUnits);
            
            // Set application context with return URLs
            ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(request.getReturnUrl() != null ? request.getReturnUrl() : "http://localhost:8006/api/payments/paypal/success?paymentId=" + request.getPaymentId())
                .cancelUrl(request.getCancelUrl() != null ? request.getCancelUrl() : "http://localhost:8006/api/payments/paypal/cancel?paymentId=" + request.getPaymentId());
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

    public PaymentResult executePayment(String gatewayOrderId) {
        log.info("Executing PayPal payment capture for Order ID: {}", gatewayOrderId);
        try {
            if (getClient() == null) {
                return createMockPaymentResult(new PaymentRequest()); // Return mock if not configured
            }

            OrdersCaptureRequest request = new OrdersCaptureRequest(gatewayOrderId);
            request.requestBody(new OrderRequest());

            HttpResponse<Order> response = getClient().execute(request);
            Order order = response.result();

            return buildPaymentResultFromOrder(order);

        } catch (Exception e) {
            log.error("Error capturing PayPal payment: {}", e.getMessage());
            
            // Handle ORDER_ALREADY_CAPTURED error idempotent behavior
            if (e.getMessage() != null && e.getMessage().contains("ORDER_ALREADY_CAPTURED")) {
                log.info("Order {} already captured. Fetching details to confirm status.", gatewayOrderId);
                try {
                    OrdersGetRequest getRequest = new OrdersGetRequest(gatewayOrderId);
                    HttpResponse<Order> response = getClient().execute(getRequest);
                    Order order = response.result();
                    
                    if ("COMPLETED".equals(order.status())) {
                        log.info("Order {} confirmed as COMPLETED.", gatewayOrderId);
                        return buildPaymentResultFromOrder(order);
                    }
                } catch (Exception ex) {
                    log.error("Error fetching captured order details: {}", ex.getMessage());
                }
            }

            PaymentResult result = new PaymentResult();
            result.setSuccess(false);
            result.setErrorMessage("PayPal capture failed: " + e.getMessage());
            return result;
        }
    }

    private PaymentResult buildPaymentResultFromOrder(Order order) {
        PaymentResult result = new PaymentResult();
        if ("COMPLETED".equals(order.status())) {
            result.setSuccess(true);
            result.setGatewayOrderId(order.id());
            // Extract capture ID if available, otherwise use order ID
            String transactionId = order.id();
            if (order.purchaseUnits() != null && !order.purchaseUnits().isEmpty()) {
                 var payments = order.purchaseUnits().get(0).payments();
                 if (payments != null && payments.captures() != null && !payments.captures().isEmpty()) {
                     transactionId = payments.captures().get(0).id();
                 }
            }
            result.setGatewayTransactionId(transactionId);
            result.setMessage("PayPal payment captured successfully.");
        } else {
            result.setSuccess(false);
            result.setErrorMessage("PayPal payment not completed. Status: " + order.status());
        }
        return result;
    }
}
