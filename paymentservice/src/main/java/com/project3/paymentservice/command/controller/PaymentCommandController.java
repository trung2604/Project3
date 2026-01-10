package com.project3.paymentservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.paymentservice.command.commands.CreatePaymentCommand;
import com.project3.paymentservice.command.dto.CreatePaymentRequest;
import com.project3.paymentservice.command.dto.PaymentCreationResponse;
import com.project3.paymentservice.command.dto.ProcessPaymentRequest;
import com.project3.paymentservice.command.dto.RefundRequest;
import com.project3.paymentservice.command.service.PaymentService;
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
public class PaymentCommandController {
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping("/create")
    @Operation(summary = "Create a new payment", description = "Initialize a payment for an order")
    public ResponseEntity<ApiResponseDTO<PaymentCreationResponse>> createPayment(
            @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Creating payment for orderId: {}", request.getOrderId());
            
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
            @RequestBody ProcessPaymentRequest request) {
        try {
            log.info("Processing payment: {}", paymentId);
            
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
    
    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a payment", description = "Process full or partial refund for a payment")
    public ResponseEntity<ApiResponseDTO<PaymentResult>> refundPayment(
            @PathVariable String paymentId,
            @RequestBody RefundRequest request) {
        try {
            log.info("Refunding payment: {}, amount: {}", paymentId, request.getRefundAmount());
            
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
}
