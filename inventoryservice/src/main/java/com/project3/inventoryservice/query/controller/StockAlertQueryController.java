package com.project3.inventoryservice.query.controller;

import com.project3.inventoryservice.query.dto.StockAlertResponse;
import com.project3.inventoryservice.query.queries.GetStockAlertsQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/alerts")
public class StockAlertQueryController {
    
    @Autowired
    private QueryGateway queryGateway;
    
    @GetMapping
    public List<StockAlertResponse> getAll(
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean isActive) {
        
        GetStockAlertsQuery query = new GetStockAlertsQuery(alertType, severity, isActive);
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
    }
    
    @GetMapping("/active")
    public List<StockAlertResponse> getActiveAlerts() {
        GetStockAlertsQuery query = new GetStockAlertsQuery(null, null, true);
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
    }
    
    @GetMapping("/low-stock")
    public List<StockAlertResponse> getLowStockAlerts() {
        GetStockAlertsQuery query = new GetStockAlertsQuery("LOW_STOCK", null, true);
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
    }
    
    @GetMapping("/expiry")
    public List<StockAlertResponse> getExpiryAlerts() {
        GetStockAlertsQuery query = new GetStockAlertsQuery("EXPIRY", null, true);
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
    }
    
    @GetMapping("/critical")
    public List<StockAlertResponse> getCriticalAlerts() {
        GetStockAlertsQuery query = new GetStockAlertsQuery(null, "CRITICAL", true);
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(StockAlertResponse.class)).join();
    }
}
