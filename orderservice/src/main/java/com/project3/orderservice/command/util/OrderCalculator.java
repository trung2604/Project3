package com.project3.orderservice.command.util;

import com.project3.orderservice.command.dto.OrderItemDTO;

import java.util.List;

public class OrderCalculator {
    private static final Double DEFAULT_VAT_PERCENTAGE = 10.0;
    
    public static Double calculateSubtotal(List<OrderItemDTO> orderItems) {
        return orderItems.stream()
                .mapToDouble(item -> item.getSubtotal() != null ? item.getSubtotal() : 
                    (item.getUnitPrice() != null && item.getQuantity() != null ? 
                        item.getUnitPrice() * item.getQuantity() : 0.0))
                .sum();
    }
    
    public static Double calculateDiscountAmount(Double subtotal, Double discountPercentage) {
        if (discountPercentage == null || discountPercentage <= 0) {
            return 0.0;
        }
        return subtotal * (discountPercentage / 100.0);
    }
    
    public static Double calculateVATAmount(Double subtotal, Double discountAmount, Double vatPercentage) {
        if (vatPercentage == null || vatPercentage <= 0) {
            vatPercentage = DEFAULT_VAT_PERCENTAGE;
        }
        Double amountAfterDiscount = subtotal - (discountAmount != null ? discountAmount : 0.0);
        return amountAfterDiscount * (vatPercentage / 100.0);
    }
    
    public static Double calculateTotalAmount(Double subtotal, Double discountAmount, Double vatAmount) {
        return subtotal - (discountAmount != null ? discountAmount : 0.0) + (vatAmount != null ? vatAmount : 0.0);
    }
    
    public static void calculateOrderItemSubtotal(OrderItemDTO item) {
        if (item.getUnitPrice() != null && item.getQuantity() != null) {
            item.setSubtotal(item.getUnitPrice() * item.getQuantity());
        }
    }
}

