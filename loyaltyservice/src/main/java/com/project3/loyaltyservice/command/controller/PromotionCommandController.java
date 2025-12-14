package com.project3.loyaltyservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.loyaltyservice.command.entity.Promotion;
import com.project3.loyaltyservice.command.entity.PromotionRepository;
import com.project3.loyaltyservice.command.enums.PromotionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/loyalty/promotions")
@Slf4j
public class PromotionCommandController {
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    @PostMapping
    public ResponseEntity<ApiResponseDTO<Promotion>> createPromotion(@RequestBody Promotion promotion) {
        if (promotion.getPromotionId() == null || promotion.getPromotionId().isEmpty()) {
            promotion.setPromotionId(UUID.randomUUID().toString());
        }
        
        if (promotion.getStatus() == null) {
            promotion.setStatus(PromotionStatus.ACTIVE);
        }
        
        Promotion saved = promotionRepository.save(promotion);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.created(saved, "Promotion created successfully"));
    }
    
    @PutMapping("/{promotionId}")
    public ResponseEntity<ApiResponseDTO<Promotion>> updatePromotion(
            @PathVariable String promotionId,
            @RequestBody Promotion promotion) {
        Promotion existing = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));
        
        if (promotion.getName() != null) existing.setName(promotion.getName());
        if (promotion.getDescription() != null) existing.setDescription(promotion.getDescription());
        if (promotion.getType() != null) existing.setType(promotion.getType());
        if (promotion.getStatus() != null) existing.setStatus(promotion.getStatus());
        if (promotion.getMenuItemIds() != null) existing.setMenuItemIds(promotion.getMenuItemIds());
        if (promotion.getDayOfWeek() != null) existing.setDayOfWeek(promotion.getDayOfWeek());
        if (promotion.getStartTime() != null) existing.setStartTime(promotion.getStartTime());
        if (promotion.getEndTime() != null) existing.setEndTime(promotion.getEndTime());
        if (promotion.getDiscountPercentage() != null) existing.setDiscountPercentage(promotion.getDiscountPercentage());
        if (promotion.getDiscountAmount() != null) existing.setDiscountAmount(promotion.getDiscountAmount());
        if (promotion.getMaxDiscountAmount() != null) existing.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
        if (promotion.getMinOrderAmount() != null) existing.setMinOrderAmount(promotion.getMinOrderAmount());
        if (promotion.getPointsMultiplier() != null) existing.setPointsMultiplier(promotion.getPointsMultiplier());
        if (promotion.getValidFrom() != null) existing.setValidFrom(promotion.getValidFrom());
        if (promotion.getValidTo() != null) existing.setValidTo(promotion.getValidTo());
        
        Promotion saved = promotionRepository.save(existing);
        return ResponseEntity.ok(ApiResponseDTO.success(saved, "Promotion updated successfully"));
    }
    
    @DeleteMapping("/{promotionId}")
    public ResponseEntity<ApiResponseDTO<Void>> deletePromotion(@PathVariable String promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new IllegalArgumentException("Promotion not found");
        }
        promotionRepository.deleteById(promotionId);
        return ResponseEntity.ok(ApiResponseDTO.noContent("Promotion deleted successfully"));
    }
}

