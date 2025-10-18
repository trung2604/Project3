package com.project3.inventoryservice.command.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, String> {
    
    List<StockAlert> findByIsActiveTrueOrderByAlertDateDesc();
    
    List<StockAlert> findByIngredientIdAndIsActiveTrue(String ingredientId);
    
    List<StockAlert> findByAlertTypeAndIsActiveTrue(String alertType);
    
    List<StockAlert> findBySeverityAndIsActiveTrue(String severity);
    
    @Query("SELECT sa FROM StockAlert sa WHERE sa.isActive = true AND " +
           "(:alertType IS NULL OR sa.alertType = :alertType) AND " +
           "(:severity IS NULL OR sa.severity = :severity)")
    List<StockAlert> findActiveAlertsByTypeAndSeverity(@Param("alertType") String alertType,
                                                      @Param("severity") String severity);
}
