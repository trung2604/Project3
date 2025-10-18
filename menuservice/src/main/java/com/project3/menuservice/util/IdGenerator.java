package com.project3.menuservice.util;

import java.util.UUID;

public class IdGenerator {
    
    public static String generateMenuItemId() {
        return "item-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    public static String generateCategoryId() {
        return "cat-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    public static String generateComboId() {
        return "combo-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    public static String generateOrderId() {
        return "order-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
