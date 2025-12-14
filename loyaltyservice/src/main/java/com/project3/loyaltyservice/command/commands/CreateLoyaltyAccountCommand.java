package com.project3.loyaltyservice.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoyaltyAccountCommand {
    private String accountId;
    private String userId;
    private String tier = "BRONZE";
}

