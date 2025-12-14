package com.project3.loyaltyservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.loyaltyservice.command.entity.Promotion;
import com.project3.loyaltyservice.command.entity.PromotionRepository;
import com.project3.loyaltyservice.command.enums.PromotionStatus;
import com.project3.loyaltyservice.command.enums.PromotionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty/promotions")
@Slf4j
public class PromotionQueryController {
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<Promotion>>> getAllPromotions(
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(required = false) PromotionType type) {
        List<Promotion> promotions;
        if (status != null && type != null) {
            promotions = promotionRepository.findByStatusAndType(status, type);
        } else if (status != null) {
            promotions = promotionRepository.findByStatus(status);
        } else if (type != null) {
            promotions = promotionRepository.findByType(type);
        } else {
            promotions = promotionRepository.findAll();
        }
        
        return ResponseEntity.ok(ApiResponseDTO.success(promotions, "Promotions retrieved successfully"));
    }
    
    @GetMapping("/{promotionId}")
    public ResponseEntity<ApiResponseDTO<Promotion>> getPromotionById(@PathVariable String promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));
        
        return ResponseEntity.ok(ApiResponseDTO.success(promotion, "Promotion retrieved successfully"));
    }
}

