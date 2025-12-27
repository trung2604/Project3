package com.project3.loyaltyservice.command.event;

import com.project3.loyaltyservice.command.entity.*;
import com.project3.loyaltyservice.command.enums.PointsTransactionType;
import com.project3.loyaltyservice.command.enums.VoucherUsageStatus;
import com.project3.loyaltyservice.command.events.LoyaltyAccountCreatedEvent;
import com.project3.loyaltyservice.command.events.PointsEarnedEvent;
import com.project3.loyaltyservice.command.events.VoucherRedeemedEvent;
import com.project3.loyaltyservice.command.service.LoyaltyEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class LoyaltyEventHandler {
    
    @Autowired
    private LoyaltyAccountRepository loyaltyAccountRepository;
    
    @Autowired
    private PointsTransactionRepository pointsTransactionRepository;
    
    @Autowired
    private VoucherUsageRepository voucherUsageRepository;
    
    @Autowired
    private LoyaltyEventPublisher eventPublisher;
    
    @EventHandler
    public void on(LoyaltyAccountCreatedEvent event) {
        log.info("Handling LoyaltyAccountCreatedEvent: {}", event.getAccountId());
        
        LoyaltyAccount account = new LoyaltyAccount();
        account.setAccountId(event.getAccountId());
        account.setUserId(event.getUserId());
        account.setTier(event.getTier());
        account.setCurrentPoints(0L);
        account.setTotalPointsEarned(0L);
        account.setTotalPointsRedeemed(0L);
        
        loyaltyAccountRepository.save(account);
        log.info("Loyalty account created: {}", event.getAccountId());
    }
    
    @EventHandler
    public void on(PointsEarnedEvent event) {
        log.info("Handling PointsEarnedEvent: accountId={}, points={}", 
            event.getAccountId(), event.getPoints());
        
        // Update loyalty account using entity method
        LoyaltyAccount account = loyaltyAccountRepository.findById(event.getAccountId())
            .orElseThrow(() -> new IllegalStateException("Loyalty account not found: " + event.getAccountId()));
        
        account.updatePointsFromEvent(event.getPoints(), event.getPointsAfter());
        loyaltyAccountRepository.save(account);
        
        // Create points transaction
        PointsTransaction transaction = new PointsTransaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setAccountId(event.getAccountId());
        transaction.setUserId(event.getUserId());
        transaction.setType(event.getPoints() > 0 ? PointsTransactionType.EARNED : PointsTransactionType.REDEEMED);
        transaction.setPoints(event.getPoints());
        transaction.setPointsBefore(event.getPointsBefore());
        transaction.setPointsAfter(event.getPointsAfter());
        transaction.setOrderId(event.getOrderId());
        transaction.setDescription(event.getDescription());
        
        pointsTransactionRepository.save(transaction);
        log.info("Points transaction saved: {}", transaction.getTransactionId());
        
        // Publish event to Kafka for notification service (only for positive points - earned)
        if (event.getPoints() > 0) {
            eventPublisher.publishPointsEarned(event);
        }
    }
    
    @EventHandler
    public void on(VoucherRedeemedEvent event) {
        log.info("Handling VoucherRedeemedEvent: accountId={}, voucherId={}, points={}", 
            event.getAccountId(), event.getVoucherId(), event.getPointsRedeemed());
        
        // Create voucher usage record
        VoucherUsage usage = new VoucherUsage();
        usage.setUsageId(UUID.randomUUID().toString());
        usage.setVoucherId(event.getVoucherId());
        usage.setUserId(event.getUserId());
        usage.setAccountId(event.getAccountId());
        usage.setOrderId(event.getOrderId());
        usage.setStatus(VoucherUsageStatus.USED);
        usage.setPointsRedeemed(event.getPointsRedeemed());
        
        voucherUsageRepository.save(usage);
        log.info("Voucher usage saved: {}", usage.getUsageId());
        
        // Publish event to Kafka for notification service
        eventPublisher.publishVoucherRedeemed(event);
    }
}

