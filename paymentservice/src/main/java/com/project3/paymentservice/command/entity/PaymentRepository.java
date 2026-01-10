package com.project3.paymentservice.command.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByOrderId(String orderId);
    List<Payment> findByCustomerId(String customerId);
    Optional<Payment> findByTransactionReference(String transactionReference);
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
}
