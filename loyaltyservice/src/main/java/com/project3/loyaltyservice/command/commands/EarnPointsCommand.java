package com.project3.loyaltyservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EarnPointsCommand {
    private String accountId;
    private String userId;
    private Long points;
    private String orderId;
    private String description;
}

