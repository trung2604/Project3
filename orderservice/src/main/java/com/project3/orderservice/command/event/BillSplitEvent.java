package com.project3.orderservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillSplitEvent {
    private String originalOrderId;
    private List<String> newOrderIds;
    private LocalDateTime splitAt;
    private String splitBy;
}

