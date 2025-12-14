package com.project3.loyaltyservice.command.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRedeemedEvent {
    private String accountId;
    private String userId;
    private String voucherId;
    private Long pointsRedeemed;
    private Long pointsBefore;
    private Long pointsAfter;
    private String orderId;
    private LocalDateTime redeemedAt;
}

