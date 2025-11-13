package com.project3.commonservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryRequestEvent {
    private String orderId;
    private String deliveryAddress;
    private String customerPhone;
    private String customerName;
    private Double totalAmount;
    private Long timestamp;
}

