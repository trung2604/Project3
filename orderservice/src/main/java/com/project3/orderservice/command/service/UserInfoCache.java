package com.project3.orderservice.command.service;

import com.project3.commonservice.dto.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class UserInfoCache {
    
    private final Map<String, CachedUserInfo> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    
    public void put(String userId, UserInfo userInfo) {
        cache.put(userId, new CachedUserInfo(userInfo, System.currentTimeMillis()));
        log.debug("Cached user info for userId: {}", userId);
    }
    
    public UserInfo get(String userId) {
        CachedUserInfo cached = cache.get(userId);
        if (cached == null) {
            return null;
        }
        
        long age = System.currentTimeMillis() - cached.timestamp;
        if (age > CACHE_TTL_MS) {
            cache.remove(userId);
            log.debug("User info cache expired for userId: {}", userId);
            return null;
        }
        
        return cached.userInfo;
    }
    
    public void remove(String userId) {
        cache.remove(userId);
        log.debug("Removed user info from cache for userId: {}", userId);
    }
    
    public void clear() {
        cache.clear();
        log.debug("Cleared user info cache");
    }
    
    private static class CachedUserInfo {
        final UserInfo userInfo;
        final long timestamp;
        
        CachedUserInfo(UserInfo userInfo, long timestamp) {
            this.userInfo = userInfo;
            this.timestamp = timestamp;
        }
    }
}

