package com.project3.loyaltyservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Parser for order data from Kafka messages
 * Encapsulates parsing logic for better cohesion and type safety
 */
@Component
@Slf4j
public class OrderDataParser {
    
    /**
     * Parses order data from Kafka message
     * 
     * @param orderData Raw order data map
     * @return Parsed OrderData object, or null if invalid
     */
    public OrderData parse(Map<String, Object> orderData) {
        String orderId = getString(orderData, "orderId");
        String customerId = getString(orderData, "customerId");
        Double totalAmount = getDouble(orderData, "totalAmount");
        
        if (orderId == null || customerId == null || totalAmount == null) {
            log.warn("Invalid order data: missing required fields. orderId={}, customerId={}, totalAmount={}", 
                orderId, customerId, totalAmount);
            return null;
        }
        
        if (totalAmount <= 0) {
            log.warn("Invalid totalAmount: {} for order {}", totalAmount, orderId);
            return null;
        }
        
        return new OrderData(orderId, customerId, totalAmount);
    }
    
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                log.error("Invalid {} format: {}", key, value);
                return null;
            }
        }
        
        log.error("Unexpected {} type: {}", key, value.getClass().getName());
        return null;
    }
    
    /**
     * Immutable data class for parsed order data
     */
    public static class OrderData {
        private final String orderId;
        private final String customerId;
        private final Double totalAmount;
        
        public OrderData(String orderId, String customerId, Double totalAmount) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.totalAmount = totalAmount;
        }
        
        public String getOrderId() {
            return orderId;
        }
        
        public String getCustomerId() {
            return customerId;
        }
        
        public Double getTotalAmount() {
            return totalAmount;
        }
    }
}

