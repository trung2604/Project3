package com.project3.loyaltyservice.command.service;

import com.project3.loyaltyservice.command.commands.CreateLoyaltyAccountCommand;
import com.project3.loyaltyservice.command.entity.LoyaltyAccount;
import com.project3.loyaltyservice.command.entity.LoyaltyAccountRepository;
import com.project3.loyaltyservice.command.constants.LoyaltyConstants;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing loyalty accounts
 * Encapsulates account creation and retrieval logic for better cohesion
 */
@Service
@Slf4j
public class LoyaltyAccountService {
    
    @Autowired
    private LoyaltyAccountRepository loyaltyAccountRepository;
    
    @Autowired
    private CommandGateway commandGateway;
    
    /**
     * Finds or creates a loyalty account for a user
     * If account doesn't exist, creates one automatically with retry logic
     * 
     * @param userId The user ID
     * @return The loyalty account (never null if creation succeeds)
     * @throws RuntimeException if account creation fails after retries
     */
    public LoyaltyAccount findOrCreateAccount(String userId) {
        Optional<LoyaltyAccount> accountOpt = loyaltyAccountRepository.findByUserId(userId);
        
        if (accountOpt.isPresent()) {
            return accountOpt.get();
        }
        
        // Auto-create loyalty account
        log.info("No loyalty account found for user: {}, auto-creating one...", userId);
        return createAccountWithRetry(userId);
    }
    
    /**
     * Creates a loyalty account with retry logic to ensure it's persisted
     */
    private LoyaltyAccount createAccountWithRetry(String userId) {
        CreateLoyaltyAccountCommand createCommand = new CreateLoyaltyAccountCommand();
        createCommand.setAccountId(UUID.randomUUID().toString());
        createCommand.setUserId(userId);
        createCommand.setTier(LoyaltyConstants.DEFAULT_TIER);
        
        try {
            commandGateway.sendAndWait(createCommand);
            
            // Retry logic to wait for event to be processed
            int retryCount = 0;
            while (retryCount < LoyaltyConstants.MAX_RETRIES) {
                Optional<LoyaltyAccount> accountOpt = loyaltyAccountRepository.findByUserId(userId);
                if (accountOpt.isPresent()) {
                    log.info("Loyalty account auto-created for user: {}", userId);
                    return accountOpt.get();
                }
                Thread.sleep(LoyaltyConstants.RETRY_DELAY_MS);
                retryCount++;
            }
            
            log.error("Failed to auto-create loyalty account for user {} after {} retries", 
                userId, LoyaltyConstants.MAX_RETRIES);
            throw new RuntimeException("Failed to create loyalty account after retries");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for loyalty account creation for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Interrupted during account creation", e);
        } catch (Exception e) {
            log.error("Failed to auto-create loyalty account for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create loyalty account", e);
        }
    }
}

