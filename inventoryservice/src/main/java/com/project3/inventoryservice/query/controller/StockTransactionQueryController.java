package com.project3.inventoryservice.query.controller;

import com.project3.inventoryservice.query.dto.StockTransactionResponse;
import com.project3.inventoryservice.query.queries.GetStockTransactionsQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/transactions")
public class StockTransactionQueryController {
    
    @Autowired
    private QueryGateway queryGateway;
    
    @GetMapping
    public List<StockTransactionResponse> getAll(
            @RequestParam(required = false) String ingredientId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        LocalDateTime from = fromDate != null ? LocalDateTime.parse(fromDate) : null;
        LocalDateTime to = toDate != null ? LocalDateTime.parse(toDate) : null;
        
        GetStockTransactionsQuery query = new GetStockTransactionsQuery(
            ingredientId, transactionType, from, to, page, size
        );
        
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockTransactionResponse.class)).join();
    }
    
    @GetMapping("/ingredient/{ingredientId}")
    public List<StockTransactionResponse> getByIngredient(
            @PathVariable String ingredientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        GetStockTransactionsQuery query = new GetStockTransactionsQuery(
            ingredientId, null, null, null, page, size
        );
        
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockTransactionResponse.class)).join();
    }
    
    @GetMapping("/type/{transactionType}")
    public List<StockTransactionResponse> getByType(
            @PathVariable String transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        GetStockTransactionsQuery query = new GetStockTransactionsQuery(
            null, transactionType, null, null, page, size
        );
        
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockTransactionResponse.class)).join();
    }
}
