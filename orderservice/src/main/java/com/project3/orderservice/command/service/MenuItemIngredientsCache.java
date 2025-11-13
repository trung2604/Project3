package com.project3.orderservice.command.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MenuItemIngredientsCache {
    
    private final Map<String, CachedIngredients> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes
    
    public void put(String menuItemId, List<String> ingredients) {
        cache.put(menuItemId, new CachedIngredients(ingredients, System.currentTimeMillis()));
        log.debug("Cached ingredients for menu item: {}", menuItemId);
    }
    
    public List<String> get(String menuItemId) {
        CachedIngredients cached = cache.get(menuItemId);
        if (cached == null) {
            return null;
        }
        
        long age = System.currentTimeMillis() - cached.timestamp;
        if (age > CACHE_TTL_MS) {
            cache.remove(menuItemId);
            log.debug("Ingredients cache expired for menu item: {}", menuItemId);
            return null;
        }
        
        return cached.ingredients;
    }
    
    public void remove(String menuItemId) {
        cache.remove(menuItemId);
        log.debug("Removed ingredients from cache for menu item: {}", menuItemId);
    }
    
    private static class CachedIngredients {
        final List<String> ingredients;
        final long timestamp;
        
        CachedIngredients(List<String> ingredients, long timestamp) {
            this.ingredients = ingredients;
            this.timestamp = timestamp;
        }
    }
}

