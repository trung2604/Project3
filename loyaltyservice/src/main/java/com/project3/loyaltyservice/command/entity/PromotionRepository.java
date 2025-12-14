package com.project3.loyaltyservice.command.entity;

import com.project3.loyaltyservice.command.enums.PromotionStatus;
import com.project3.loyaltyservice.command.enums.PromotionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, String> {
    List<Promotion> findByStatus(PromotionStatus status);
    List<Promotion> findByType(PromotionType type);
    List<Promotion> findByStatusAndType(PromotionStatus status, PromotionType type);
}

