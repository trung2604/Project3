package com.project3.orderservice.query.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRespone {
    private String orderId;

    private String customerId;

    private String orderStatus;

    private Double orderAmount;

    private String orderDate;

    private Long vat;
}
