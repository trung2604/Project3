package com.project3.orderservice.command.constants;

/**
 * Constants for Order Service
 */
public final class OrderConstants {
    
    private OrderConstants() {
        // Utility class
    }
    
    // Kafka Topics
    public static final String TOPIC_PAYMENT_REQUEST = "payment-request";
    public static final String TOPIC_DELIVERY_REQUEST = "delivery-request";
    public static final String TOPIC_INVENTORY_DEDUCTION_REQUEST = "inventory-deduction-request";
    public static final String TOPIC_ORDER_COMPLETED = "order-completed";
    
    // Default values
    public static final String DEFAULT_UNIT = "kg";
    public static final String DEFAULT_INVENTORY_REASON = "Order cooking - automatic deduction";
    
    // Order Status Names (for string comparison)
    public static final String STATUS_COOKING = "COOKING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_COMPLETED = "COMPLETED";
    
    // Order Type Names
    public static final String TYPE_DELIVERY = "DELIVERY";
    public static final String TYPE_UNKNOWN = "UNKNOWN";
}

