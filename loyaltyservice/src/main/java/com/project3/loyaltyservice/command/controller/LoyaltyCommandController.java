package com.project3.loyaltyservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.loyaltyservice.command.commands.CreateLoyaltyAccountCommand;
import com.project3.loyaltyservice.command.commands.EarnPointsCommand;
import com.project3.loyaltyservice.command.commands.RedeemVoucherCommand;
import com.project3.loyaltyservice.command.entity.LoyaltyAccount;
import com.project3.loyaltyservice.command.entity.LoyaltyAccountRepository;
import com.project3.loyaltyservice.command.entity.Voucher;
import com.project3.loyaltyservice.command.entity.VoucherRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/loyalty")
@Slf4j
public class LoyaltyCommandController extends BaseLoyaltyController {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private LoyaltyAccountRepository loyaltyAccountRepository;
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponseDTO<String>> createLoyaltyAccount(
            @RequestBody CreateLoyaltyAccountCommand command,
            HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        
        // Check if account already exists
        if (loyaltyAccountRepository.findByUserId(currentUserId).isPresent()) {
            throw new IllegalStateException("Loyalty account already exists for this user");
        }
        
        if (command.getAccountId() == null || command.getAccountId().isEmpty()) {
            command.setAccountId(UUID.randomUUID().toString());
        }
        command.setUserId(currentUserId);
        
        String result = commandGateway.sendAndWait(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.created(result, "Loyalty account created successfully"));
    }
    
    @PostMapping("/points/earn")
    public ResponseEntity<ApiResponseDTO<String>> earnPoints(
            @RequestBody EarnPointsCommand command,
            HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(currentUserId)
            .orElseThrow(() -> new IllegalStateException("Loyalty account not found"));
        
        command.setAccountId(account.getAccountId());
        command.setUserId(currentUserId);
        
        String result = commandGateway.sendAndWait(command);
        return ResponseEntity.ok(ApiResponseDTO.success(result, 
            "Points earned successfully: +" + command.getPoints()));
    }
    
    @PostMapping("/vouchers/{voucherId}/redeem")
    public ResponseEntity<ApiResponseDTO<String>> redeemVoucher(
            @PathVariable String voucherId,
            @RequestParam(required = false) String orderId,
            HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(currentUserId)
            .orElseThrow(() -> new IllegalStateException("Loyalty account not found"));
        
        Voucher voucher = voucherRepository.findById(voucherId)
            .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        
        // Validate voucher redemption using entity method
        voucher.validateRedemption(account);
        
        RedeemVoucherCommand command = new RedeemVoucherCommand();
        command.setAccountId(account.getAccountId());
        command.setUserId(currentUserId);
        command.setVoucherId(voucherId);
        command.setPointsRequired(voucher.getPointsRequired());
        command.setOrderId(orderId);
        
        String result = commandGateway.sendAndWait(command);
        
        // Redeem voucher using entity method
        voucher.redeem();
        voucherRepository.save(voucher);
        
        return ResponseEntity.ok(ApiResponseDTO.success(result, 
            "Voucher redeemed successfully"));
    }
}

