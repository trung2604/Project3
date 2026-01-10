package com.project3.paymentservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundRequest {
    private Double refundAmount;
    private String reason;
    private String requestedBy;
}
