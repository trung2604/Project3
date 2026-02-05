package com.project3.paymentservice.command.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.paymentservice.command.enums.PaymentMethod;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class VietQRPaymentProcessor implements PaymentProcessor {
    
    // VietQR API Constants
    private static final int QR_TYPE_DYNAMIC = 0;
    private static final String TRANS_TYPE_CREDIT = "C";
    private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 30;
    private static final int CONTENT_MAX_LENGTH = 23;
    private static final int ORDER_ID_MAX_LENGTH = 13;
    private static final String TOKEN_ENDPOINT = "/api/token_generate";
    private static final String QR_GENERATE_ENDPOINT = "/api/qr/generate-customer";
    
    @Value("${vietqr.api-url}")
    private String apiUrl;
    
    @Value("${vietqr.username:}")
    private String username;
    
    @Value("${vietqr.password:}")
    private String password;
    
    @Value("${vietqr.bank-id}")
    private String bankId;
    
    @Value("${vietqr.account-no:}")
    private String accountNo;
    
    @Value("${vietqr.account-name:}")
    private String accountName;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // Thread-safe token cache
    private final AtomicReference<TokenCache> tokenCache = new AtomicReference<>();
    
    public VietQRPaymentProcessor(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    /**
     * Validate configuration at startup
     */
    @PostConstruct
    public void validateConfiguration() {
        if (!isConfigured()) {
            log.warn("=====================================================================");
            log.warn("VietQR Payment Processor is NOT fully configured!");
            log.warn("Missing required properties:");
            if (!isNotEmpty(apiUrl)) log.warn("  - vietqr.api-url");
            if (!isNotEmpty(username)) log.warn("  - vietqr.username");
            if (!isNotEmpty(password)) log.warn("  - vietqr.password");
            if (!isNotEmpty(bankId)) log.warn("  - vietqr.bank-id");
            if (!isNotEmpty(accountNo)) log.warn("  - vietqr.account-no");
            if (!isNotEmpty(accountName)) log.warn("  - vietqr.account-name");
            log.warn("VietQR will use MOCK mode until properly configured.");
            log.warn("=====================================================================");
        } else {
            log.info("VietQR Payment Processor configured successfully.");
            log.info("  API URL: {}", apiUrl);
            log.info("  Bank ID: {}", bankId);
            log.info("  Account: {}***", accountNo != null && accountNo.length() > 4 
                ? accountNo.substring(0, 4) : "****");
        }
    }
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Processing VietQR payment for orderId: {}", request.getOrderId());
        
        try {
            String description = "Order " + request.getOrderId();
            String qrData = generateVietQR(request.getAmount(), description, request.getPaymentId());
            
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            result.setGatewayOrderId("VIETQR-" + request.getOrderId());
            result.setGatewayTransactionId("VIETQR-TXN-" + UUID.randomUUID());
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
    
    /**
     * Get Bearer Token from VietQR API using Basic Authentication
     * Token is cached and reused until it expires (thread-safe)
     */
    private String getBearerToken() {
        try {
            // Check cached token
            TokenCache cached = tokenCache.get();
            if (cached != null && cached.isValid()) {
                log.debug("Using cached VietQR Bearer Token");
                return cached.token;
            }
            
            // Generate Basic Auth header
            String credentials = username + ":" + password;
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
            );
            
            log.info("Requesting new VietQR Bearer Token from {}{}", apiUrl, TOKEN_ENDPOINT);
            
            // Call token generation API
            String response = webClient.post()
                .uri(apiUrl + TOKEN_ENDPOINT)
                .header("Authorization", basicAuth)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response);
            String accessToken = jsonResponse.get("access_token").asText();
            int expiresIn = jsonResponse.get("expires_in").asInt();
            
            // Cache the new token (thread-safe)
            Instant expiryTime = Instant.now().plusSeconds(expiresIn - TOKEN_EXPIRY_BUFFER_SECONDS);
            tokenCache.set(new TokenCache(accessToken, expiryTime));
            
            log.info("VietQR Bearer Token obtained successfully, expires in {} seconds", expiresIn);
            return accessToken;
            
        } catch (WebClientResponseException e) {
            log.error("VietQR API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get VietQR authentication token: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error obtaining VietQR Bearer Token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get VietQR authentication token", e);
        }
    }
    
    private String generateVietQR(Double amount, String description, String paymentId) {
        try {
            // Validate configuration
            if (!isConfigured()) {
                log.warn("VietQR not fully configured, returning mock QR data");
                return createMockQRData(amount, description, paymentId);
            }
            
            // Get Bearer Token
            String bearerToken = getBearerToken();
            
            // Build request body with correct parameters per VietQR API spec
            Map<String, Object> requestBody = buildRequestBody(amount, description, paymentId);
            
            log.info("Calling VietQR generate-customer API - Amount: {}, OrderId: {}", amount, paymentId);
            log.debug("Request: bankCode={}, bankAccount={}, userBankName={}, content={}, qrType={}", 
                bankId, accountNo, accountName, requestBody.get("content"), QR_TYPE_DYNAMIC);
            
            // Call VietQR API
            String response = webClient.post()
                .uri(apiUrl + QR_GENERATE_ENDPOINT)
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("VietQR API returned empty response");
                return createMockQRData(amount, description, paymentId);
            }
            
            try {
                // Parse JSON to extract qrDataURL
                JsonNode root = objectMapper.readTree(response);
                String code = root.path("code").asText();
                
                if ("00".equals(code)) {
                    JsonNode dataNode = root.path("data");
                    if (!dataNode.isMissingNode()) {
                        String qrDataURL = dataNode.path("qrDataURL").asText();
                        if (qrDataURL != null && !qrDataURL.isEmpty()) {
                            log.info("Extracted qrDataURL from VietQR response");
                            return qrDataURL;
                        }
                    }
                } else {
                     log.warn("VietQR API returned error code: {} - {}", code, root.path("desc").asText());
                }
            } catch (Exception e) {
                log.warn("Failed to parse VietQR response JSON: {}", e.getMessage());
                // Fallback to returning raw response if not valid JSON or parsing failed
            }
            
            log.info("VietQR API response received successfully");
            return response;
            
        } catch (WebClientResponseException e) {
            log.error("VietQR API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            log.warn("Falling back to mock QR data");
            return createMockQRData(amount, description, paymentId);
        } catch (Exception e) {
            log.error("Error calling VietQR API: {}", e.getMessage(), e);
            log.warn("Falling back to mock QR data");
            return createMockQRData(amount, description, paymentId);
        }
    }
    
    /**
     * Check if VietQR is fully configured
     */
    private boolean isConfigured() {
        return isNotEmpty(accountNo) && 
               isNotEmpty(username) && 
               isNotEmpty(password) && 
               isNotEmpty(bankId) &&
               isNotEmpty(accountName);
    }
    
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Build VietQR API request body
     */
    private Map<String, Object> buildRequestBody(Double amount, String description, String paymentId) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // Required parameters
        requestBody.put("bankCode", bankId);
        requestBody.put("bankAccount", accountNo);
        requestBody.put("userBankName", accountName.toUpperCase());
        requestBody.put("content", sanitizeAndTruncate(description, CONTENT_MAX_LENGTH));
        requestBody.put("qrType", QR_TYPE_DYNAMIC);
        
        // Required for dynamic QR (qrType = 0)
        requestBody.put("amount", amount.longValue());
        requestBody.put("orderId", truncate(paymentId, ORDER_ID_MAX_LENGTH));
        
        // Optional but recommended
        requestBody.put("transType", TRANS_TYPE_CREDIT);
        
        return requestBody;
    }
    
    /**
     * Remove Vietnamese accents and special characters, then truncate
     */
    private String sanitizeAndTruncate(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        // Normalize and remove diacritical marks
        String normalized = Normalizer.normalize(content, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{M}", "");
        
        // Handle đ and Đ (not covered by NFD normalization)
        String sanitized = withoutAccents
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .replaceAll("[^a-zA-Z0-9 ]", "");
        
        return truncate(sanitized, maxLength);
    }
    
    /**
     * Truncate string to max length
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
    
    private String createMockQRData(Double amount, String description, String paymentId) {
        // Generate a valid VietQR image URL using the public API
        // Format: https://img.vietqr.io/image/<BANK_ID>-<ACCOUNT_NO>-<TEMPLATE>.png?amount=<AMOUNT>&addInfo=<CONTENT>&accountName=<NAME>
        
        String cleanDesc = sanitizeAndTruncate("Order " + paymentId, 50);
        String safeAccountName = accountName != null ? accountName.replace(" ", "%20") : "MOCK";
        
        return String.format("https://img.vietqr.io/image/%s-%s-compact.png?amount=%.0f&addInfo=%s&accountName=%s", 
            bankId != null ? bankId : "970436", 
            accountNo != null ? accountNo : "1016892621", 
            amount, 
            cleanDesc.replace(" ", "%20"),
            safeAccountName);
    }
    
    @Override
    public PaymentResult processRefund(String paymentId, Double amount, String reason) {
        log.info("Processing VietQR refund for paymentId: {}, amount: {}", paymentId, amount);
        
        // VietQR refunds are typically manual via bank
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setGatewayTransactionId("VIETQR-REFUND-" + UUID.randomUUID());
        result.setMessage("VietQR refund recorded. Manual bank transfer required for amount: " + amount);
        
        return result;
    }
    
    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.VIETQR;
    }
    
    /**
     * Thread-safe token cache holder
     */
    private static class TokenCache {
        final String token;
        final Instant expiryTime;
        
        TokenCache(String token, Instant expiryTime) {
            this.token = token;
            this.expiryTime = expiryTime;
        }
        
        boolean isValid() {
            return Instant.now().isBefore(expiryTime);
        }
    }
}
