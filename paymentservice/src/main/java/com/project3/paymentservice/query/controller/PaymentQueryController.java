package com.project3.paymentservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.paymentservice.command.entity.Payment;
import com.project3.paymentservice.command.entity.PaymentRepository;
import com.project3.paymentservice.query.queries.GetPaymentByIdQuery;
import com.project3.paymentservice.query.queries.GetPaymentsByOrderIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Query", description = "Payment query endpoints")
@Slf4j
public class PaymentQueryController {
    
    @Autowired
    private QueryGateway queryGateway;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details by payment ID")
    public ResponseEntity<ApiResponseDTO<Payment>> getPaymentById(@PathVariable String paymentId) {
        try {
            log.info("Querying payment by ID: {}", paymentId);
            
            Payment payment = queryGateway.query(
                new GetPaymentByIdQuery(paymentId),
                Payment.class
            ).join();
            
            if (payment != null) {
                return ResponseEntity.ok(ApiResponseDTO.success(payment, "Payment found"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound("Payment not found"));
            }
            
        } catch (Exception e) {
            log.error("Error querying payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to retrieve payment: " + e.getMessage()));
        }
    }
    
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payments by order ID", description = "Retrieve all payments for an order")
    public ResponseEntity<ApiResponseDTO<List<Payment>>> getPaymentsByOrderId(@PathVariable String orderId) {
        try {
            log.info("Querying payments for orderId: {}", orderId);
            
            List<Payment> payments = queryGateway.query(
                new GetPaymentsByOrderIdQuery(orderId),
                List.class
            ).join();
            
            return ResponseEntity.ok(ApiResponseDTO.success(payments, "Payments retrieved"));
            
        } catch (Exception e) {
            log.error("Error querying payments for order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to retrieve payments: " + e.getMessage()));
        }
    }
    
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get payments by customer ID", description = "Retrieve all payments for a customer")
    public ResponseEntity<ApiResponseDTO<List<Payment>>> getPaymentsByCustomerId(@PathVariable String customerId) {
        try {
            log.info("Querying payments for customerId: {}", customerId);
            
            List<Payment> payments = paymentRepository.findByCustomerId(customerId);
            
            return ResponseEntity.ok(ApiResponseDTO.success(payments, "Payments retrieved"));
            
        } catch (Exception e) {
            log.error("Error querying payments for customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to retrieve payments: " + e.getMessage()));
        }
    }
    
    @GetMapping("")
    @Operation(summary = "Get all payments", description = "Retrieve all payments (for admin)")
    public ResponseEntity<ApiResponseDTO<List<Payment>>> getAllPayments() {
        try {
            log.info("Querying all payments");
            
            List<Payment> payments = paymentRepository.findAll();
            
            return ResponseEntity.ok(ApiResponseDTO.success(payments, "Payments retrieved"));
            
        } catch (Exception e) {
            log.error("Error querying all payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to retrieve payments: " + e.getMessage()));
        }
    }
}
