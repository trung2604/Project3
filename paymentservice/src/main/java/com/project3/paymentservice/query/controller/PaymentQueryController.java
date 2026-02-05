package com.project3.paymentservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.commonservice.dto.UserInfo;
import com.project3.paymentservice.command.controller.BasePaymentController;
import com.project3.paymentservice.command.entity.Payment;
import com.project3.paymentservice.command.entity.PaymentRepository;
import com.project3.paymentservice.query.queries.GetPaymentByIdQuery;
import com.project3.paymentservice.query.queries.GetPaymentsByOrderIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
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
public class PaymentQueryController extends BasePaymentController {
    
    @Autowired
    private QueryGateway queryGateway;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details by payment ID")
    public ResponseEntity<ApiResponseDTO<Payment>> getPaymentById(
            @PathVariable String paymentId,
            HttpServletRequest httpRequest) {
        try {
            // 1. Validate user authentication
            UserInfo currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("Authentication required");
            }
            
            String currentUserId = getCurrentUserId(httpRequest);
            
            log.info("Querying payment by ID: {} by user: {}", paymentId, currentUserId);
            
            Payment payment = queryGateway.query(
                new GetPaymentByIdQuery(paymentId),
                Payment.class
            ).join();
            
            if (payment != null) {
                // 2. Authorization check - only payment owner or STAFF+ can view
                if (!canAccessPayment(payment, currentUserId, currentUser)) {
                    return forbidden("Access denied");
                }
                
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
    public ResponseEntity<ApiResponseDTO<List<Payment>>> getPaymentsByOrderId(
            @PathVariable String orderId,
            HttpServletRequest httpRequest) {
        try {
            // 1. Validate user authentication
            UserInfo currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("Authentication required");
            }
            
            log.info("Querying payments for orderId: {}", orderId);
            
            List<Payment> payments = queryGateway.query(
                new GetPaymentsByOrderIdQuery(orderId),
                ResponseTypes.multipleInstancesOf(Payment.class)
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
    public ResponseEntity<ApiResponseDTO<List<Payment>>> getPaymentsByCustomerId(
            @PathVariable String customerId,
            HttpServletRequest httpRequest) {
        try {
            // 1. Validate user authentication
            UserInfo currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("Authentication required");
            }
            
            String currentUserId = getCurrentUserId(httpRequest);
            
            // 2. Authorization - only access own payments or STAFF+
            if (!customerId.equals(currentUserId) && !isStaffOrAbove(currentUser)) {
                return forbidden("You can only view your own payments");
            }
            
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
    public ResponseEntity<ApiResponseDTO<List<Payment>>> getAllPayments(HttpServletRequest httpRequest) {
        try {
            // 1. Validate user authentication
            UserInfo currentUser = validateUser(httpRequest);
            if (currentUser == null) {
                return unauthorized("Authentication required");
            }
            
            // 2. Only ADMIN and MANAGER can view all payments
            if (!isAdminOrManager(currentUser)) {
                return forbidden("Administrator access required");
            }
            
            log.info("Querying all payments by admin: {}", getCurrentUserId(httpRequest));
            
            List<Payment> payments = paymentRepository.findAll();
            
            return ResponseEntity.ok(ApiResponseDTO.success(payments, "Payments retrieved"));
            
        } catch (Exception e) {
            log.error("Error querying all payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.internalServerError("Failed to retrieve payments: " + e.getMessage()));
        }
    }
}
