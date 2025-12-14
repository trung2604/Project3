package com.project3.loyaltyservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.commonservice.security.SecurityUtils;
import com.project3.loyaltyservice.command.entity.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@Slf4j
public class LoyaltyQueryController {
    
    @Autowired
    private LoyaltyAccountRepository loyaltyAccountRepository;
    
    @Autowired
    private PointsTransactionRepository pointsTransactionRepository;
    
    @Autowired
    private VoucherUsageRepository voucherUsageRepository;
    
    @GetMapping("/accounts/me")
    public ResponseEntity<ApiResponseDTO<LoyaltyAccount>> getMyLoyaltyAccount(HttpServletRequest request) {
        String userId = SecurityUtils.getUserIdFromHeader(request);
        
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Loyalty account not found"));
        
        return ResponseEntity.ok(ApiResponseDTO.success(account, "Loyalty account retrieved successfully"));
    }
    
    @GetMapping("/accounts/{userId}")
    public ResponseEntity<ApiResponseDTO<LoyaltyAccount>> getLoyaltyAccountByUserId(@PathVariable String userId) {
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Loyalty account not found"));
        
        return ResponseEntity.ok(ApiResponseDTO.success(account, "Loyalty account retrieved successfully"));
    }
    
    @GetMapping("/points/transactions")
    public ResponseEntity<ApiResponseDTO<List<PointsTransaction>>> getPointsTransactions(HttpServletRequest request) {
        String userId = SecurityUtils.getUserIdFromHeader(request);
        
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Loyalty account not found"));
        
        List<PointsTransaction> transactions = pointsTransactionRepository
            .findByAccountIdOrderByCreatedAtDesc(account.getAccountId());
        
        return ResponseEntity.ok(ApiResponseDTO.success(transactions, "Points transactions retrieved successfully"));
    }
    
    @GetMapping("/vouchers/usage")
    public ResponseEntity<ApiResponseDTO<List<VoucherUsage>>> getVoucherUsageHistory(HttpServletRequest request) {
        String userId = SecurityUtils.getUserIdFromHeader(request);
        
        List<VoucherUsage> usages = voucherUsageRepository.findByUserIdOrderByUsedAtDesc(userId);
        
        return ResponseEntity.ok(ApiResponseDTO.success(usages, "Voucher usage history retrieved successfully"));
    }
}

