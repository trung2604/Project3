package com.project3.loyaltyservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.loyaltyservice.command.entity.Voucher;
import com.project3.loyaltyservice.command.entity.VoucherRepository;
import com.project3.loyaltyservice.command.enums.VoucherStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/loyalty/vouchers")
@Slf4j
public class VoucherQueryController {
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<Voucher>>> getAllVouchers(
            @RequestParam(required = false) VoucherStatus status) {
        List<Voucher> vouchers;
        if (status != null) {
            vouchers = voucherRepository.findByStatus(status);
        } else {
            vouchers = voucherRepository.findAll();
        }
        
        return ResponseEntity.ok(ApiResponseDTO.success(vouchers, "Vouchers retrieved successfully"));
    }
    
    @GetMapping("/{voucherId}")
    public ResponseEntity<ApiResponseDTO<Voucher>> getVoucherById(@PathVariable String voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
            .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        
        return ResponseEntity.ok(ApiResponseDTO.success(voucher, "Voucher retrieved successfully"));
    }
    
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponseDTO<Voucher>> getVoucherByCode(@PathVariable String code) {
        Voucher voucher = voucherRepository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        
        return ResponseEntity.ok(ApiResponseDTO.success(voucher, "Voucher retrieved successfully"));
    }
}

