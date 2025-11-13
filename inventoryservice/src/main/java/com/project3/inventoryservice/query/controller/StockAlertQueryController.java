package com.project3.inventoryservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.inventoryservice.query.dto.StockAlertResponse;
import com.project3.inventoryservice.query.queries.GetStockAlertsQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/alerts")
@Slf4j
public class StockAlertQueryController {
    
    @Autowired
    private QueryGateway queryGateway;
    
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<StockAlertResponse>>> getAll(
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean isActive) {
        
        try {
            GetStockAlertsQuery query = new GetStockAlertsQuery(alertType, severity, isActive);
            List<StockAlertResponse> alerts = queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(alerts, "Stock alerts retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving stock alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve stock alerts: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponseDTO<List<StockAlertResponse>>> getActiveAlerts() {
        try {
            GetStockAlertsQuery query = new GetStockAlertsQuery(null, null, true);
            List<StockAlertResponse> alerts = queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(alerts, "Active stock alerts retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving active stock alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve active stock alerts: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponseDTO<List<StockAlertResponse>>> getLowStockAlerts() {
        try {
            GetStockAlertsQuery query = new GetStockAlertsQuery("LOW_STOCK", null, true);
            List<StockAlertResponse> alerts = queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(alerts, "Low stock alerts retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving low stock alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve low stock alerts: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/expiry")
    public ResponseEntity<ApiResponseDTO<List<StockAlertResponse>>> getExpiryAlerts() {
        try {
            GetStockAlertsQuery query = new GetStockAlertsQuery("EXPIRY", null, true);
            List<StockAlertResponse> alerts = queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(alerts, "Expiry alerts retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving expiry alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve expiry alerts: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/critical")
    public ResponseEntity<ApiResponseDTO<List<StockAlertResponse>>> getCriticalAlerts() {
        try {
            GetStockAlertsQuery query = new GetStockAlertsQuery(null, "CRITICAL", true);
            List<StockAlertResponse> alerts = queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(alerts, "Critical alerts retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving critical alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve critical alerts: " + e.getMessage(), 500));
        }
    }
}
