package com.project3.loyaltyservice.command.aggregate;

import com.project3.loyaltyservice.command.commands.CreateLoyaltyAccountCommand;
import com.project3.loyaltyservice.command.commands.DeductPointsCommand;
import com.project3.loyaltyservice.command.commands.EarnPointsCommand;
import com.project3.loyaltyservice.command.commands.RedeemVoucherCommand;
import com.project3.loyaltyservice.command.events.LoyaltyAccountCreatedEvent;
import com.project3.loyaltyservice.command.events.PointsEarnedEvent;
import com.project3.loyaltyservice.command.events.VoucherRedeemedEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDateTime;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Aggregate
public class LoyaltyAccountAggregate {
    @AggregateIdentifier
    private String accountId;
    
    private String userId;
    private Long currentPoints = 0L;
    private Long totalPointsEarned = 0L;
    private Long totalPointsRedeemed = 0L;
    private String tier = "BRONZE";
    
    @CommandHandler
    public LoyaltyAccountAggregate(CreateLoyaltyAccountCommand command) {
        if (command.getAccountId() == null || command.getAccountId().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        if (command.getUserId() == null || command.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        LoyaltyAccountCreatedEvent event = new LoyaltyAccountCreatedEvent();
        event.setAccountId(command.getAccountId());
        event.setUserId(command.getUserId());
        event.setTier(command.getTier() != null ? command.getTier() : "BRONZE");
        event.setCreatedAt(LocalDateTime.now());
        
        apply(event);
    }
    
    @EventSourcingHandler
    public void on(LoyaltyAccountCreatedEvent event) {
        this.accountId = event.getAccountId();
        this.userId = event.getUserId();
        this.tier = event.getTier();
        this.currentPoints = 0L;
        this.totalPointsEarned = 0L;
        this.totalPointsRedeemed = 0L;
    }
    
    @CommandHandler
    public void handle(EarnPointsCommand command) {
        if (command.getPoints() == null || command.getPoints() <= 0) {
            throw new IllegalArgumentException("Points must be greater than 0");
        }
        
        Long pointsBefore = this.currentPoints;
        Long pointsAfter = pointsBefore + command.getPoints();
        
        PointsEarnedEvent event = new PointsEarnedEvent();
        event.setAccountId(this.accountId);
        event.setUserId(command.getUserId());
        event.setPoints(command.getPoints());
        event.setPointsBefore(pointsBefore);
        event.setPointsAfter(pointsAfter);
        event.setOrderId(command.getOrderId());
        event.setDescription(command.getDescription());
        event.setEarnedAt(LocalDateTime.now());
        
        apply(event);
    }
    
    @EventSourcingHandler
    public void on(PointsEarnedEvent event) {
        this.currentPoints = event.getPointsAfter();
        this.totalPointsEarned += event.getPoints();
    }
    
    @CommandHandler
    public void handle(DeductPointsCommand command) {
        if (command.getPoints() == null || command.getPoints() <= 0) {
            throw new IllegalArgumentException("Points to deduct must be greater than 0");
        }
        
        Long pointsBefore = this.currentPoints;
        Long pointsToDeduct = Math.min(command.getPoints(), this.currentPoints); // Don't go negative
        Long pointsAfter = pointsBefore - pointsToDeduct;
        
        if (pointsToDeduct > 0) {
            PointsEarnedEvent event = new PointsEarnedEvent();
            event.setAccountId(this.accountId);
            event.setUserId(command.getUserId());
            event.setPoints(-pointsToDeduct); // Negative for deduction
            event.setPointsBefore(pointsBefore);
            event.setPointsAfter(pointsAfter);
            event.setOrderId(command.getOrderId());
            event.setDescription(command.getReason());
            event.setEarnedAt(LocalDateTime.now());
            
            apply(event);
        }
    }
    
    @CommandHandler
    public void handle(RedeemVoucherCommand command) {
        if (command.getPointsRequired() == null || command.getPointsRequired() <= 0) {
            throw new IllegalArgumentException("Points required must be greater than 0");
        }
        if (this.currentPoints < command.getPointsRequired()) {
            throw new IllegalStateException("Insufficient points. Required: " + 
                command.getPointsRequired() + ", Available: " + this.currentPoints);
        }
        
        Long pointsBefore = this.currentPoints;
        Long pointsAfter = pointsBefore - command.getPointsRequired();
        
        PointsEarnedEvent pointsEvent = new PointsEarnedEvent();
        pointsEvent.setAccountId(this.accountId);
        pointsEvent.setUserId(command.getUserId());
        pointsEvent.setPoints(-command.getPointsRequired()); // Negative points for redemption
        pointsEvent.setPointsBefore(pointsBefore);
        pointsEvent.setPointsAfter(pointsAfter);
        pointsEvent.setOrderId(command.getOrderId());
        pointsEvent.setDescription("Redeemed voucher: " + command.getVoucherId());
        pointsEvent.setEarnedAt(LocalDateTime.now());
        
        apply(pointsEvent);
        
        VoucherRedeemedEvent voucherEvent = new VoucherRedeemedEvent();
        voucherEvent.setAccountId(this.accountId);
        voucherEvent.setUserId(command.getUserId());
        voucherEvent.setVoucherId(command.getVoucherId());
        voucherEvent.setPointsRedeemed(command.getPointsRequired());
        voucherEvent.setPointsBefore(pointsBefore);
        voucherEvent.setPointsAfter(pointsAfter);
        voucherEvent.setOrderId(command.getOrderId());
        voucherEvent.setRedeemedAt(LocalDateTime.now());
        
        apply(voucherEvent);
    }
    
    @EventSourcingHandler
    public void on(VoucherRedeemedEvent event) {
        this.currentPoints = event.getPointsAfter();
        this.totalPointsRedeemed += event.getPointsRedeemed();
    }
}

