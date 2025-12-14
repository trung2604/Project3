package com.project3.loyaltyservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedeemVoucherCommand {
    private String accountId;
    private String userId;
    private String voucherId;
    private Long pointsRequired; // Points required to redeem this voucher
    private String orderId; // Optional: order where voucher will be used
}

