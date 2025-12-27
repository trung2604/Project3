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
        try {
            String userId = SecurityUtils.getUserIdFromHeader(request);
            
            LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId)
                .orElse(null);
            
            if (account == null) {
                // Return empty account instead of throwing exception
                return ResponseEntity.ok(ApiResponseDTO.success(null, "Loyalty account not found. Please create one first."));
            }
            
            return ResponseEntity.ok(ApiResponseDTO.success(account, "Loyalty account retrieved successfully"));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error retrieving loyalty account: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error("Failed to retrieve loyalty account: " + e.getMessage(), 400));
        }
    }
    
    @GetMapping("/accounts/{userId}")
    public ResponseEntity<ApiResponseDTO<LoyaltyAccount>> getLoyaltyAccountByUserId(@PathVariable String userId) {
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Loyalty account not found"));
        
        return ResponseEntity.ok(ApiResponseDTO.success(account, "Loyalty account retrieved successfully"));
    }
    
    @GetMapping("/points/transactions")
    public ResponseEntity<ApiResponseDTO<List<PointsTransaction>>> getPointsTransactions(HttpServletRequest request) {
        try {
            String userId = SecurityUtils.getUserIdFromHeader(request);
            
            LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId)
                .orElse(null);
            
            if (account == null) {
                // Return empty list instead of throwing exception
                return ResponseEntity.ok(ApiResponseDTO.success(java.util.Collections.emptyList(), "No loyalty account found"));
            }
            
            List<PointsTransaction> transactions = pointsTransactionRepository
                .findByAccountIdOrderByCreatedAtDesc(account.getAccountId());
            
            return ResponseEntity.ok(ApiResponseDTO.success(transactions, "Points transactions retrieved successfully"));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error retrieving points transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error("Failed to retrieve points transactions: " + e.getMessage(), 400));
        }
    }
    
    @GetMapping("/vouchers/usage")
    public ResponseEntity<ApiResponseDTO<List<VoucherUsage>>> getVoucherUsageHistory(HttpServletRequest request) {
        try {
            String userId = SecurityUtils.getUserIdFromHeader(request);
            
            List<VoucherUsage> usages = voucherUsageRepository.findByUserIdOrderByUsedAtDesc(userId);
            
            return ResponseEntity.ok(ApiResponseDTO.success(usages, "Voucher usage history retrieved successfully"));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponseDTO.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error retrieving voucher usage history: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error("Failed to retrieve voucher usage history: " + e.getMessage(), 400));
        }
    }
}

