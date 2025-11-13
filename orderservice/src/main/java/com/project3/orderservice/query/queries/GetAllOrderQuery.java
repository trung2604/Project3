package com.project3.orderservice.query.queries;

import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetAllOrderQuery {
    private OrderStatus status;
    private OrderType type;
    private String customerId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
