package com.project3.paymentservice.command.entity;

import com.project3.paymentservice.command.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String transactionId;
    
    @Column(nullable = false)
    private String paymentId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(length = 2000)
    private String gatewayResponse;
    
    private String gatewayTransactionId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private String status;
    
    @Column(length = 1000)
    private String metadata;
    
    private String errorMessage;
}
