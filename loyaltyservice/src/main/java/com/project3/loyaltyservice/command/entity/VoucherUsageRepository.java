package com.project3.loyaltyservice.command.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, String> {
    List<VoucherUsage> findByUserIdOrderByUsedAtDesc(String userId);
    List<VoucherUsage> findByVoucherId(String voucherId);
    List<VoucherUsage> findByOrderId(String orderId);
}

