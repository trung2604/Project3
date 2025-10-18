package com.project3.inventoryservice.service;

import com.project3.inventoryservice.command.entity.Ingredient;
import com.project3.inventoryservice.command.entity.IngredientRepository;
import com.project3.inventoryservice.command.entity.StockAlert;
import com.project3.inventoryservice.command.entity.StockAlertRepository;
import com.project3.inventoryservice.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertService {
    
    @Autowired
    private IngredientRepository ingredientRepository;
    
    @Autowired
    private StockAlertRepository stockAlertRepository;
    
    // Check for low stock alerts every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void checkLowStockAlerts() {
        List<Ingredient> lowStockIngredients = ingredientRepository.findLowStockIngredients();
        
        for (Ingredient ingredient : lowStockIngredients) {
            // Check if alert already exists
            List<StockAlert> existingAlerts = stockAlertRepository.findByIngredientIdAndIsActiveTrue(ingredient.getIngredientId());
            boolean hasLowStockAlert = existingAlerts.stream()
                .anyMatch(alert -> "LOW_STOCK".equals(alert.getAlertType()));
            
            if (!hasLowStockAlert) {
                StockAlert alert = new StockAlert();
                alert.setAlertId(IdGenerator.generateAlertId());
                alert.setIngredientId(ingredient.getIngredientId());
                alert.setAlertType("LOW_STOCK");
                alert.setSeverity(ingredient.getCurrentStock() <= 0 ? "CRITICAL" : "HIGH");
                alert.setMessage("Low stock alert for " + ingredient.getName() + 
                    ". Current: " + ingredient.getCurrentStock() + 
                    ", Min: " + ingredient.getMinStockLevel());
                alert.setAlertDate(LocalDateTime.now());
                alert.setIsActive(true);
                alert.setCurrentStock(ingredient.getCurrentStock());
                alert.setMinStockLevel(ingredient.getMinStockLevel());
                
                stockAlertRepository.save(alert);
            }
        }
    }
    
    // Check for expiry alerts every day at 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkExpiryAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(7); // Warning 7 days before expiry
        
        List<Ingredient> expiringIngredients = ingredientRepository.findExpiringIngredients(warningDate);
        
        for (Ingredient ingredient : expiringIngredients) {
            // Check if alert already exists
            List<StockAlert> existingAlerts = stockAlertRepository.findByIngredientIdAndIsActiveTrue(ingredient.getIngredientId());
            boolean hasExpiryAlert = existingAlerts.stream()
                .anyMatch(alert -> "EXPIRY".equals(alert.getAlertType()));
            
            if (!hasExpiryAlert) {
                String severity = "MEDIUM";
                String message;
                
                if (ingredient.getExpiryDate().isBefore(today)) {
                    severity = "CRITICAL";
                    message = "Ingredient " + ingredient.getName() + " has EXPIRED on " + ingredient.getExpiryDate();
                } else if (ingredient.getExpiryDate().equals(today)) {
                    severity = "HIGH";
                    message = "Ingredient " + ingredient.getName() + " expires TODAY (" + ingredient.getExpiryDate() + ")";
                } else {
                    long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, ingredient.getExpiryDate());
                    message = "Ingredient " + ingredient.getName() + " expires in " + daysUntilExpiry + " days (" + ingredient.getExpiryDate() + ")";
                }
                
                StockAlert alert = new StockAlert();
                alert.setAlertId(IdGenerator.generateAlertId());
                alert.setIngredientId(ingredient.getIngredientId());
                alert.setAlertType("EXPIRY");
                alert.setSeverity(severity);
                alert.setMessage(message);
                alert.setAlertDate(LocalDateTime.now());
                alert.setIsActive(true);
                alert.setExpiryDate(ingredient.getExpiryDate().toString());
                
                stockAlertRepository.save(alert);
            }
        }
    }
}
