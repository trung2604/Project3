package com.project3.paymentservice.command.service.payment;

import com.project3.paymentservice.command.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class VietQRPaymentProcessor implements PaymentProcessor {
    
    @Value("${vietqr.api-url}")
    private String apiUrl;
    
    @Value("${vietqr.client-id:}")
    private String clientId;
    
    @Value("${vietqr.api-key:}")
    private String apiKey;
    
    @Value("${vietqr.bank-id}")
    private String bankId;
    
    @Value("${vietqr.account-no:}")
    private String accountNo;
    
    @Value("${vietqr.account-name:}")
    private String accountName;
    
    @Value("${vietqr.template:compact}")
    private String template;
    
    private final WebClient webClient;
    
    public VietQRPaymentProcessor() {
        this.webClient = WebClient.builder().build();
    }
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Processing VietQR payment for orderId: {}", request.getOrderId());
        
        try {
            // Generate VietQR payment
            String description = "Order " + request.getOrderId();
            String qrData = generateVietQR(request.getAmount(), description, request.getPaymentId());
            
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            result.setGatewayOrderId("VIETQR-" + request.getOrderId());
            result.setGatewayTransactionId("VIETQR-TXN-" + UUID.randomUUID().toString());
            result.setQrCodeData(qrData);
            result.setMessage("VietQR code generated. Awaiting customer scan and payment.");
            
            log.info("VietQR generated successfully for orderId: {}", request.getOrderId());
            return result;
            
        } catch (Exception e) {
            log.error("Error processing VietQR payment: {}", e.getMessage(), e);
            PaymentResult result = new PaymentResult();
            result.setSuccess(false);
            result.setErrorMessage("VietQR generation failed: " + e.getMessage());
            return result;
        }
    }
    
    private String generateVietQR(Double amount, String description, String paymentId) {
        try {
            // Check if API is configured
            if (accountNo == null || accountNo.isEmpty()) {
                log.warn("VietQR not configured, returning mock QR data");
                return createMockQRData(amount, description, paymentId);
            }
            
            // Build VietQR API request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("accountNo", accountNo);
            requestBody.put("accountName", accountName);
            requestBody.put("acqId", bankId);
            requestBody.put("amount", amount.intValue());
            requestBody.put("addInfo", description);
            requestBody.put("format", "text");
            requestBody.put("template", template);
            
            // Call VietQR API
            String response = webClient.post()
                .uri(apiUrl + "/generate")
                .header("x-client-id", clientId)
                .header("x-api-key", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            log.info("VietQR API response received");
            return response;
            
        } catch (Exception e) {
            log.error("Error calling VietQR API, returning mock data: {}", e.getMessage());
            return createMockQRData(amount, description, paymentId);
        }
    }
    
    private String createMockQRData(Double amount, String description, String paymentId) {
        // Create a simple QR data string for testing
        return String.format("VietQR|Bank:%s|Account:%s|Amount:%.0f|Desc:%s|Ref:%s", 
            bankId, accountNo != null ? accountNo : "MOCK", amount, description, paymentId);
    }
    
    @Override
    public PaymentResult processRefund(String paymentId, Double amount, String reason) {
        log.info("Processing VietQR refund for paymentId: {}, amount: {}", paymentId, amount);
        
        // VietQR refunds are typically manual via bank
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setGatewayTransactionId("VIETQR-REFUND-" + UUID.randomUUID().toString());
        result.setMessage("VietQR refund recorded. Manual bank transfer required for amount: " + amount);
        
        return result;
    }
    
    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.VIETQR;
    }
}
