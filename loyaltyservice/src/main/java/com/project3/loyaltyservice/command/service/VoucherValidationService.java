package com.project3.loyaltyservice.command.service;

import com.project3.loyaltyservice.command.entity.LoyaltyAccount;
import com.project3.loyaltyservice.command.entity.Voucher;
import com.project3.loyaltyservice.command.enums.VoucherStatus;
import org.springframework.stereotype.Service;

/**
 * Service for validating voucher redemption
 * Encapsulates validation logic for better cohesion
 * 
 * Note: Validation logic has been moved to Voucher entity.
 * This service now only provides a convenience method.
 */
@Service
public class VoucherValidationService {
    
    /**
     * Validates if a voucher can be redeemed using entity method
     * @throws IllegalStateException if validation fails
     */
    public void validateVoucherRedemption(Voucher voucher, LoyaltyAccount account) {
        voucher.validateRedemption(account);
    }
}

