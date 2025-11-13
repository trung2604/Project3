package com.project3.commonservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestEvent {
    private String orderId;
    private Double amount;
    private String customerId;
    private String orderType;
    private Long timestamp;
}

