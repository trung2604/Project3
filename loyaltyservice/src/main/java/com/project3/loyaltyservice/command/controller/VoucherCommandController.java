package com.project3.loyaltyservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.loyaltyservice.command.entity.Voucher;
import com.project3.loyaltyservice.command.entity.VoucherRepository;
import com.project3.loyaltyservice.command.enums.VoucherStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/loyalty/vouchers")
@Slf4j
public class VoucherCommandController {
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @PostMapping
    public ResponseEntity<ApiResponseDTO<Voucher>> createVoucher(@RequestBody Voucher voucher) {
        if (voucher.getVoucherId() == null || voucher.getVoucherId().isEmpty()) {
            voucher.setVoucherId(UUID.randomUUID().toString());
        }
        
        if (voucher.getCode() == null || voucher.getCode().isEmpty()) {
            voucher.setCode("VOUCHER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        
        if (voucher.getStatus() == null) {
            voucher.setStatus(VoucherStatus.ACTIVE);
        }
        
        if (voucher.getRemainingQuantity() == null) {
            voucher.setRemainingQuantity(voucher.getTotalQuantity());
        }
        
        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.created(saved, "Voucher created successfully"));
    }
    
    @PutMapping("/{voucherId}")
    public ResponseEntity<ApiResponseDTO<Voucher>> updateVoucher(
            @PathVariable String voucherId,
            @RequestBody Voucher voucher) {
        Voucher existing = voucherRepository.findById(voucherId)
            .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        
        if (voucher.getName() != null) existing.setName(voucher.getName());
        if (voucher.getDescription() != null) existing.setDescription(voucher.getDescription());
        if (voucher.getPointsRequired() != null) existing.setPointsRequired(voucher.getPointsRequired());
        if (voucher.getDiscountAmount() != null) existing.setDiscountAmount(voucher.getDiscountAmount());
        if (voucher.getDiscountPercentage() != null) existing.setDiscountPercentage(voucher.getDiscountPercentage());
        if (voucher.getMaxDiscountAmount() != null) existing.setMaxDiscountAmount(voucher.getMaxDiscountAmount());
        if (voucher.getMinOrderAmount() != null) existing.setMinOrderAmount(voucher.getMinOrderAmount());
        if (voucher.getValidFrom() != null) existing.setValidFrom(voucher.getValidFrom());
        if (voucher.getValidTo() != null) existing.setValidTo(voucher.getValidTo());
        if (voucher.getStatus() != null) existing.setStatus(voucher.getStatus());
        
        Voucher saved = voucherRepository.save(existing);
        return ResponseEntity.ok(ApiResponseDTO.success(saved, "Voucher updated successfully"));
    }
    
    @DeleteMapping("/{voucherId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteVoucher(@PathVariable String voucherId) {
        if (!voucherRepository.existsById(voucherId)) {
            throw new IllegalArgumentException("Voucher not found");
        }
        voucherRepository.deleteById(voucherId);
        return ResponseEntity.ok(ApiResponseDTO.noContent("Voucher deleted successfully"));
    }
}

