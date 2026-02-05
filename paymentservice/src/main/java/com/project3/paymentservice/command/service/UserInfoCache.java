package com.project3.paymentservice.command.service;

import com.project3.commonservice.dto.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory cache for user information
 * Reduces API calls to UserService
 */
@Component
@Slf4j
public class UserInfoCache {
    
    private final ConcurrentMap<String, UserInfo> cache = new ConcurrentHashMap<>();
    
    /**
     * Gets user info from cache
     */
    public UserInfo get(String userId) {
        return cache.get(userId);
    }
    
    /**
     * Puts user info into cache
     */
    public void put(String userId, UserInfo userInfo) {
        cache.put(userId, userInfo);
        log.debug("User info cached for userId: {}", userId);
    }
    
    /**
     * Removes user info from cache
     */
    public void remove(String userId) {
        cache.remove(userId);
        log.debug("User info removed from cache for userId: {}", userId);
    }
    
    /**
     * Clears all cached entries
     */
    public void clear() {
        cache.clear();
        log.info("User info cache cleared");
    }
    
    /**
     * Gets cache size
     */
    public int size() {
        return cache.size();
    }
}
