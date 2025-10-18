package com.project3.inventoryservice.query.projection;

import com.project3.inventoryservice.command.entity.StockAlert;
import com.project3.inventoryservice.command.entity.StockAlertRepository;
import com.project3.inventoryservice.command.entity.IngredientRepository;
import com.project3.inventoryservice.query.dto.StockAlertResponse;
import com.project3.inventoryservice.query.queries.GetStockAlertsQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StockAlertProjection {
    
    @Autowired
    private StockAlertRepository stockAlertRepository;
    
    @Autowired
    private IngredientRepository ingredientRepository;
    
    @QueryHandler
    public List<StockAlertResponse> getAll(GetStockAlertsQuery query) {
        List<StockAlert> alerts;
        
        if (query.getAlertType() != null && query.getSeverity() != null) {
            alerts = stockAlertRepository.findActiveAlertsByTypeAndSeverity(
                query.getAlertType(), 
                query.getSeverity()
            );
        } else if (query.getAlertType() != null) {
            alerts = stockAlertRepository.findByAlertTypeAndIsActiveTrue(query.getAlertType());
        } else if (query.getSeverity() != null) {
            alerts = stockAlertRepository.findBySeverityAndIsActiveTrue(query.getSeverity());
        } else if (query.getIsActive() != null && query.getIsActive()) {
            alerts = stockAlertRepository.findByIsActiveTrueOrderByAlertDateDesc();
        } else {
            alerts = stockAlertRepository.findAll();
        }
        
        return alerts.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    private StockAlertResponse mapToResponse(StockAlert alert) {
        StockAlertResponse response = new StockAlertResponse();
        response.setAlertId(alert.getAlertId());
        response.setIngredientId(alert.getIngredientId());
        
        // Get ingredient name
        String ingredientName = ingredientRepository.findById(alert.getIngredientId())
            .map(ingredient -> ingredient.getName())
            .orElse("Unknown");
        response.setIngredientName(ingredientName);
        
        response.setAlertType(alert.getAlertType());
        response.setSeverity(alert.getSeverity());
        response.setMessage(alert.getMessage());
        response.setAlertDate(alert.getAlertDate());
        response.setAcknowledgedAt(alert.getAcknowledgedAt());
        response.setAcknowledgedBy(alert.getAcknowledgedBy());
        response.setIsActive(alert.getIsActive());
        response.setCurrentStock(alert.getCurrentStock());
        response.setMinStockLevel(alert.getMinStockLevel());
        response.setExpiryDate(alert.getExpiryDate());
        return response;
    }
}
