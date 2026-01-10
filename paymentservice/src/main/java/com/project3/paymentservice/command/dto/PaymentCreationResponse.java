package com.project3.paymentservice.command.dto;

import com.project3.paymentservice.command.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCreationResponse {
    private String paymentId;
    private String orderId;
    private Double amount;
    private PaymentMethod paymentMethod;
    private String status;
}
