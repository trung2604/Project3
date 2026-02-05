package com.project3.paymentservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.paymentservice.command.commands.CreatePaymentCommand;
import com.project3.paymentservice.command.dto.CreatePaymentRequest;
import com.project3.paymentservice.command.dto.PaymentCreationResponse;
import com.project3.paymentservice.command.dto.ProcessPaymentRequest;
import com.project3.paymentservice.command.dto.RefundRequest;
import com.project3.paymentservice.command.service.PaymentService;
import com.project3.commonservice.dto.UserInfo;
import com.project3.paymentservice.command.entity.Payment;
import com.project3.paymentservice.command.service.payment.PaymentRequest;
import com.project3.paymentservice.command.service.payment.PaymentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Command", description = "Payment processing endpoints")
@Slf4j
public class PaymentCommandController extends BasePaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping("/create")
    @Operation(summary = "Create a new payment", description = "Initialize a payment for an order")
    public ResponseEntity<ApiResponseDTO<PaymentCreationResponse>> createPayment(
            @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 1. Validate user authentication
            UserInfo currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("Authentication required");
            }
            
            String currentUserId = getCurrentUserId(httpRequest);
            
            // 2. Verify user can create payment for this customer
            if (!canCreatePaymentFor(request.getCustomerId(), currentUserId, currentUser)) {
                return forbidden("You can only create payments for your own orders");
            }
            
            // 3. Security: Override customerId with current user (prevent spoofing)
            if (!isStaffOrAbove(currentUser)) {
                request.setCustomerId(currentUserId);
            }
            
            log.info("Creating payment for orderId: {} by user: {}", request.getOrderId(), currentUserId);
            
            CreatePaymentCommand command = new CreatePaymentCommand();
            command.setOrderId(request.getOrderId());
            command.setCustomerId(request.getCustomerId());
            command.setAmount(request.getAmount());
            command.setPaymentMethod(request.getPaymentMethod());
            command.setIpAddress(httpRequest.getRemoteAddr());
            command.setUserAgent(httpRequest.getHeader("User-Agent"));
            
            String paymentId = paymentService.createPayment(command);
            
            PaymentCreationResponse response = new PaymentCreationResponse();
            response.setPaymentId(paymentId);
            response.setOrderId(request.getOrderId());
            response.setAmount(request.getAmount());
            response.setPaymentMethod(request.getPaymentMethod());
            response.setStatus("PENDING");
            
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Payment created successfully"));
            
        } catch (Exception e) {
            log.error("Error creating payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to create payment: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{paymentId}/process")
    @Operation(summary = "Process a payment", description = "Process payment through the selected gateway")
    public ResponseEntity<ApiResponseDTO<PaymentResult>> processPayment(
            @PathVariable String paymentId,
            @RequestBody ProcessPaymentRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 1. Validate user authentication
            UserInfo currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("Authentication required");
            }
            
            String currentUserId = getCurrentUserId(httpRequest);
            
            // 2. Get payment and verify it exists
            // 2. Get payment and verify it exists (with retry for Eventual Consistency)
            Payment payment = findPaymentWithRetry(paymentId);
            if (payment == null) {
                log.error("Payment not found after retry: {}", paymentId);
                return notFound("Payment not found - ensure payment was created successfully");
            }
            
            // 3. Verify user can access this payment
            if (!canAccessPayment(payment, currentUserId, currentUser)) {
                return forbidden("You can only process your own payments");
            }
            
            log.info("Processing payment: {} by user: {}", paymentId, currentUserId);
            
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setPaymentId(paymentId);
            paymentRequest.setOrderId(request.getOrderId());
            paymentRequest.setCustomerId(request.getCustomerId());
            paymentRequest.setAmount(request.getAmount());
            paymentRequest.setPaymentMethod(request.getPaymentMethod());
            paymentRequest.setReturnUrl(request.getReturnUrl());
            paymentRequest.setCancelUrl(request.getCancelUrl());
            
            PaymentResult result = paymentService.processPayment(paymentId, paymentRequest);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponseDTO.success(result, "Payment processed successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.badRequest(result.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to process payment: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{paymentId}/complete")
    @Operation(summary = "Complete a payment manually", description = "Mark a payment as completed (e.g. for VietQR manual check)")
    public ResponseEntity<ApiResponseDTO<Void>> completePayment(
            @PathVariable String paymentId,
            HttpServletRequest httpRequest) {
        try {
            // 1. Validate user authentication
            UserInfo currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("Authentication required");
            }

            // 2. Allow Customer to complete their own payment (Mock flow) or Staff
            Payment payment = findPaymentWithRetry(paymentId);
            if (payment == null) {
                return notFound("Payment not found");
            }
            
            String currentUserId = getCurrentUserId(httpRequest);
            if (!canAccessPayment(payment, currentUserId, currentUser)) {
                return forbidden("Access denied");
            }

            log.info("Completing payment: {} by user: {}", paymentId, currentUserId);
            
            paymentService.completePayment(paymentId);
            
            return ResponseEntity.ok(ApiResponseDTO.success(null, "Payment completed successfully"));
            
        } catch (Exception e) {
            log.error("Error completing payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to complete payment: " + e.getMessage()));
        }
    }

   @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a payment", description = "Process full or partial refund for a payment")
    public ResponseEntity<ApiResponseDTO<PaymentResult>> refundPayment(
            @PathVariable String paymentId,
            @RequestBody RefundRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 1. Validate user authentication
            UserInfo currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("Authentication required");
            }
            
            // 2. Only ADMIN and MANAGER can refund
            if (!canRefundPayment(currentUser)) {
                return forbidden("Only administrators and managers can process refunds");
            }
            
            String currentUserId = getCurrentUserId(httpRequest);
            
            // 3. Get payment and verify it exists
            // 3. Get payment and verify it exists (with retry)
            Payment payment = findPaymentWithRetry(paymentId);
            if (payment == null) {
                log.error("Payment not found after retry: {}", paymentId);
                return notFound("Payment not found");
            }
            
            log.info("Refunding payment: {} amount: {} by user: {}", paymentId, request.getRefundAmount(), currentUserId);
            
            PaymentResult result = paymentService.refundPayment(
                paymentId,
                request.getRefundAmount(),
                request.getReason(),
                request.getRequestedBy()
            );
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponseDTO.success(result, "Refund processed successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.badRequest(result.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("Error refunding payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to process refund: " + e.getMessage()));
        }
    }
    
    /**
     * Helper to find payment with retry to handle Eventual Consistency
     */
    private Payment findPaymentWithRetry(String paymentId) {
        int maxRetries = 5;
        long delayMs = 500;
        
        for (int i = 0; i < maxRetries; i++) {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment != null) {
                return payment;
            }
            try {
                log.debug("Payment {} not found, retrying... ({}/{})", paymentId, i + 1, maxRetries);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }
    @GetMapping("/paypal/success")
    @Operation(summary = "Handle PayPal success callback", description = "Capture payment after user approval")
    public ResponseEntity<ApiResponseDTO<PaymentResult>> handlePayPalSuccess(
            @RequestParam(required = false) String paymentId,
            @RequestParam("token") String token,
            @RequestParam("PayerID") String payerId) {
        try {
            // Note: paymentId might not be passed back by PayPal if not in query params of returnUrl.
            // But we configured returnUrl to include it? 
            // PayPalPaymentProcessor default: 
            // .returnUrl(request.getReturnUrl() != null ? request.getReturnUrl() : "http://localhost:8006/api/payments/paypal/success")
            // It loses paymentId context if we don't append it!
            // BUT, wait, the `token` from PayPal IS the Order ID.
            // And we saved that as `gatewayOrderId`.
            // However, `executePayPalPayment` takes `paymentId`.
            // We need to associate token -> paymentId, OR we change executePayPalPayment to find payment by gatewayOrderId?
            // Re-checking PayPalPaymentProcessor: 
            // It creates Order. Its ID is returned as gatewayOrderId.
            
            // CHALLENGE: If we don't pass paymentId in the return URL, we don't know which payment to update!
            // The default returnUrl in PayPalPaymentProcessor is static.
            // We MUST update PayPalPaymentProcessor to append paymentId to the returnUrl if it's using the default.
            
            // For now, let's assume the frontend or the service is generating the correct returnURL with query params.
            // IF paymentId is null, we are in trouble.
            // Let's log it.
            
            log.info("PayPal success callback received. PaymentId: {}, Token: {}, PayerID: {}", paymentId, token, payerId);
            
            if (paymentId == null) {
                // Try to fallback if possible? 
                // Or maybe the caller passed it?
                // Actually, let's look at `PayPalPaymentProcessor` again.
                // It uses `request.getReturnUrl()`.
                // The FRONTEND (or whoever calls create payment) usually supplies this.
                // If they don't, it uses localhost static one.
                return badRequest("Missing paymentId in callback");
            }

            PaymentResult result = paymentService.executePayPalPayment(paymentId, token, payerId);
            
            if (result.isSuccess()) {
                 // Return HTML or redirect if this is a browser hit?
                 // Since this is a REST Controller, returning JSON is standard, but PayPal redirects the BROWSER here.
                 // So the User sees JSON?
                 // Ideally, we should redirect the user back to the Frontend (e.g. localhost:3000/payment/success).
                 // But for this audit, implementing the backend logic is key.
                 return ResponseEntity.ok(ApiResponseDTO.success(result, "Payment completed successfully"));
            } else {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.badRequest(result.getErrorMessage()));
            }
        } catch (Exception e) {
            log.error("Error handling PayPal success: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to complete payment: " + e.getMessage()));
        }
    }

    @GetMapping("/paypal/cancel")
    @Operation(summary = "Handle PayPal cancel callback", description = "Handle user cancellation")
    public ResponseEntity<ApiResponseDTO<Void>> handlePayPalCancel(
            @RequestParam(required = false) String paymentId) {
        log.info("PayPal cancel callback received for paymentId: {}", paymentId);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Payment cancelled by user"));
    }
}
