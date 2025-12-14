package com.project3.loyaltyservice.command.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsEarnedEvent {
    private String accountId;
    private String userId;
    private Long points;
    private Long pointsBefore;
    private Long pointsAfter;
    private String orderId;
    private String description;
    private LocalDateTime earnedAt;
}

