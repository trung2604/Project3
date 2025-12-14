package com.project3.loyaltyservice.command.service;

import com.project3.loyaltyservice.command.constants.LoyaltyConstants;
import org.springframework.stereotype.Service;

/**
 * Service for calculating loyalty points
 * Encapsulates points calculation logic for better cohesion
 */
@Service
public class PointsCalculator {
    
    /**
     * Calculates points based on order amount
     * Formula: 1 point per 10,000 VND, minimum 1 point
     * 
     * @param totalAmount Order total amount in VND
     * @return Calculated points (minimum 1)
     */
    public Long calculatePoints(Double totalAmount) {
        if (totalAmount == null || totalAmount <= 0) {
            return LoyaltyConstants.MIN_POINTS;
        }
        
        long points = (long) Math.floor(totalAmount / LoyaltyConstants.POINTS_PER_AMOUNT);
        return Math.max(LoyaltyConstants.MIN_POINTS, points);
    }
    
    /**
     * Generates description for points earned from an order
     */
    public String generatePointsDescription(String orderId, Double totalAmount) {
        return String.format("Points earned from order #%s (Amount: %.0f VND)", orderId, totalAmount);
    }
}

