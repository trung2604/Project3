package com.project3.inventoryservice.query.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetStockTransactionsQuery {
    private String ingredientId;
    private String transactionType;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private int page;
    private int size;
}
