package com.project3.inventoryservice.command.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, String> {
    
    List<StockTransaction> findByIngredientIdOrderByTransactionDateDesc(String ingredientId);
    
    List<StockTransaction> findByTransactionTypeOrderByTransactionDateDesc(String transactionType);
    
    @Query("SELECT st FROM StockTransaction st WHERE " +
           "(:ingredientId IS NULL OR st.ingredientId = :ingredientId) AND " +
           "(:transactionType IS NULL OR st.transactionType = :transactionType) AND " +
           "(:fromDate IS NULL OR st.transactionDate >= :fromDate) AND " +
           "(:toDate IS NULL OR st.transactionDate <= :toDate)")
    Page<StockTransaction> findByFilters(@Param("ingredientId") String ingredientId,
                                        @Param("transactionType") String transactionType,
                                        @Param("fromDate") LocalDateTime fromDate,
                                        @Param("toDate") LocalDateTime toDate,
                                        Pageable pageable);
    
    @Query("SELECT st FROM StockTransaction st WHERE st.ingredientId = :ingredientId " +
           "ORDER BY st.transactionDate DESC LIMIT 1")
    StockTransaction findLatestTransactionByIngredientId(@Param("ingredientId") String ingredientId);
}
