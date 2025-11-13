package com.project3.inventoryservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.inventoryservice.query.dto.StockTransactionResponse;
import com.project3.inventoryservice.query.queries.GetStockTransactionsQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/transactions")
@Slf4j
public class StockTransactionQueryController {
    
    @Autowired
    private QueryGateway queryGateway;
    
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<StockTransactionResponse>>> getAll(
            @RequestParam(required = false) String ingredientId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            LocalDateTime from = fromDate != null ? LocalDateTime.parse(fromDate) : null;
            LocalDateTime to = toDate != null ? LocalDateTime.parse(toDate) : null;
            
            GetStockTransactionsQuery query = new GetStockTransactionsQuery(
                ingredientId, transactionType, from, to, page, size
            );
            
            List<StockTransactionResponse> transactions = queryGateway.query(
                query, 
                ResponseTypes.multipleInstancesOf(StockTransactionResponse.class)
            ).join();
            
            return ResponseEntity.ok(ApiResponseDTO.success(transactions, "Stock transactions retrieved successfully"));
        } catch (java.time.format.DateTimeParseException e) {
            log.error("Invalid date format in getAll transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Invalid date format. Use ISO format: yyyy-MM-ddTHH:mm:ss", 400));
        } catch (Exception e) {
            log.error("Error in getAll transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve transactions: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/ingredient/{ingredientId}")
    public ResponseEntity<ApiResponseDTO<List<StockTransactionResponse>>> getByIngredient(
            @PathVariable String ingredientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            GetStockTransactionsQuery query = new GetStockTransactionsQuery(
                ingredientId, null, null, null, page, size
            );
            
            List<StockTransactionResponse> transactions = queryGateway.query(
                query, 
                ResponseTypes.multipleInstancesOf(StockTransactionResponse.class)
            ).join();
            
            return ResponseEntity.ok(ApiResponseDTO.success(transactions, "Stock transactions retrieved successfully by ingredient: " + ingredientId));
        } catch (Exception e) {
            log.error("Error in getByIngredient transactions for ingredient {}: {}", ingredientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve transactions: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/type/{transactionType}")
    public ResponseEntity<ApiResponseDTO<List<StockTransactionResponse>>> getByType(
            @PathVariable String transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            GetStockTransactionsQuery query = new GetStockTransactionsQuery(
                null, transactionType, null, null, page, size
            );
            
            List<StockTransactionResponse> transactions = queryGateway.query(
                query, 
                ResponseTypes.multipleInstancesOf(StockTransactionResponse.class)
            ).join();
            
            return ResponseEntity.ok(ApiResponseDTO.success(transactions, "Stock transactions retrieved successfully by type: " + transactionType));
        } catch (Exception e) {
            log.error("Error in getByType transactions for type {}: {}", transactionType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve transactions: " + e.getMessage(), 500));
        }
    }
}
