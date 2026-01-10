package com.project3.paymentservice.command.entity;

import com.project3.paymentservice.command.enums.PaymentMethod;
import com.project3.paymentservice.command.enums.PaymentStatus;
import com.project3.paymentservice.command.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    private String paymentId;
    
    @Column(nullable = false)
    private String orderId;
    
    @Column(nullable = false)
    private String customerId;
    
    @Column(nullable = false)
    private Double amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    private String transactionReference;
    
    private String gatewayOrderId;
    
    private String gatewayTransactionId;
    
    private String gatewayResponse;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime processedAt;
    
    private LocalDateTime refundedAt;
    
    private Double refundedAmount;
    
    private String failureReason;
    
    @Column(length = 1000)
    private String notes;
    
    private String ipAddress;
    
    private String userAgent;
}
