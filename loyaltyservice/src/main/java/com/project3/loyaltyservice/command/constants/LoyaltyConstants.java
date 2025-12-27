package com.project3.loyaltyservice.command.constants;

/**
 * Constants for Loyalty Service
 */
public final class LoyaltyConstants {
    
    private LoyaltyConstants() {
        // Utility class
    }
    
    // Points calculation
    public static final double POINTS_PER_AMOUNT = 10000.0; // 1 point per 10,000 VND
    public static final long MIN_POINTS = 1L;
    
    // Account creation
    public static final String DEFAULT_TIER = "BRONZE";
    
    // Retry configuration
    public static final int MAX_RETRIES = 5;
    public static final long RETRY_DELAY_MS = 200L;
    
    // Kafka Topics
    public static final String TOPIC_ORDER_COMPLETED = "order-completed";
    public static final String TOPIC_POINTS_EARNED = "loyalty-points-earned";
    public static final String TOPIC_VOUCHER_REDEEMED = "loyalty-voucher-redeemed";
}

