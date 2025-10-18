package com.project3.inventoryservice.util;

import java.util.UUID;

public class IdGenerator {

    public static String generateIngredientId() {
        return "ingredient-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String generateStockInId() {
        return "stockin-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String generateStockOutId() {
        return "stockout-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String generateAdjustmentId() {
        return "adjust-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String generateStockTakeId() {
        return "stocktake-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String generateAlertId() {
        return "alert-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
