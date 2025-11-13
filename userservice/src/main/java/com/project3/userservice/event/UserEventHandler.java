package com.project3.userservice.event;

import com.project3.commonservice.dto.UserInfoUpdatedEvent;
import com.project3.commonservice.service.KafkaService;
import com.project3.userservice.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserEventHandler {
    
    @Autowired
    private KafkaService kafkaService;
    
    @Async
    @EventListener
    public void handleUserUpdated(User user) {
        try {
            if (user == null || user.getUserId() == null) {
                return;
            }
            
            UserInfoUpdatedEvent event = new UserInfoUpdatedEvent();
            event.setUserId(user.getUserId());
            event.setUsername(user.getUsername());
            event.setEmail(user.getEmail());
            event.setRole(user.getRole() != null ? user.getRole().name() : null);
            event.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
            event.setTimestamp(System.currentTimeMillis());
            
            kafkaService.sendMessage("user-info-updated", event);
            log.info("Published user info updated event for userId: {}", user.getUserId());
            
        } catch (Exception e) {
            log.error("Error publishing user info updated event: {}", e.getMessage(), e);
        }
    }
}

