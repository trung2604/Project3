package com.project3.inventoryservice.query.projection;

import com.project3.inventoryservice.command.entity.StockTransaction;
import com.project3.inventoryservice.command.entity.StockTransactionRepository;
import com.project3.inventoryservice.command.entity.IngredientRepository;
import com.project3.inventoryservice.query.dto.StockTransactionResponse;
import com.project3.inventoryservice.query.queries.GetStockTransactionsQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StockTransactionProjection {
    
    @Autowired
    private StockTransactionRepository stockTransactionRepository;
    
    @Autowired
    private IngredientRepository ingredientRepository;
    
    @QueryHandler
    public List<StockTransactionResponse> getAll(GetStockTransactionsQuery query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
        Page<StockTransaction> transactionPage = stockTransactionRepository.findByFilters(
            query.getIngredientId(),
            query.getTransactionType(),
            query.getFromDate(),
            query.getToDate(),
            pageable
        );
        
        return transactionPage.getContent().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    private StockTransactionResponse mapToResponse(StockTransaction transaction) {
        StockTransactionResponse response = new StockTransactionResponse();
        response.setTransactionId(transaction.getTransactionId());
        response.setIngredientId(transaction.getIngredientId());
        
        // Get ingredient name
        String ingredientName = ingredientRepository.findById(transaction.getIngredientId())
            .map(ingredient -> ingredient.getName())
            .orElse("Unknown");
        response.setIngredientName(ingredientName);
        
        response.setTransactionType(transaction.getTransactionType());
        response.setQuantity(transaction.getQuantity());
        response.setUnit(transaction.getUnit());
        response.setUnitCost(transaction.getUnitCost());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setReference(transaction.getReference());
        response.setReason(transaction.getReason());
        response.setNotes(transaction.getNotes());
        response.setCreatedBy(transaction.getCreatedBy());
        response.setStockBefore(transaction.getStockBefore());
        response.setStockAfter(transaction.getStockAfter());
        return response;
    }
}
