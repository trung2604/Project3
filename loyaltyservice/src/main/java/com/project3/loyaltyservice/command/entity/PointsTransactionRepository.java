package com.project3.loyaltyservice.command.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, String> {
    List<PointsTransaction> findByAccountIdOrderByCreatedAtDesc(String accountId);
    List<PointsTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
    List<PointsTransaction> findByOrderId(String orderId);
}

