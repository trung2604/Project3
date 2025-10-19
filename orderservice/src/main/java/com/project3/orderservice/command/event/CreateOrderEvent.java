package com.project3.orderservice.command.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderEvent {
    private String orderID;
    private String customerId;
    private String orderStatus;
    private Double orderAmount;
    private String orderDate;
    private Long vat;
}
