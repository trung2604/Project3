package com.project3.loyaltyservice.command.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyAccountCreatedEvent {
    private String accountId;
    private String userId;
    private String tier;
    private LocalDateTime createdAt;
}

