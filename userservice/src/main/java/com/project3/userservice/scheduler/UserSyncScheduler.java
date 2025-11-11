package com.project3.userservice.scheduler;

import com.project3.userservice.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.user-sync.enabled", havingValue = "true", matchIfMissing = true)
public class UserSyncScheduler {
    
    private final IUserService userService;
    
    /**
     * Sync deleted users from Keycloak every 5 minutes
     * This ensures that if a user is deleted in Keycloak dashboard,
     * they will also be deleted from the database
     */
    @Scheduled(fixedRateString = "${app.user-sync.interval:300000}") // Default: 5 minutes (300000 ms)
    public void syncDeletedUsers() {
        log.debug("Running scheduled sync: checking for users deleted in Keycloak");
        try {
            userService.syncDeletedUsersFromKeycloak();
        } catch (Exception e) {
            log.error("Error in scheduled sync: {}", e.getMessage(), e);
        }
    }
}

