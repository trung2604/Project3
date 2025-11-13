package com.project3.orderservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.commonservice.dto.UserInfo;
import com.project3.commonservice.dto.UserInfoUpdatedEvent;
import com.project3.orderservice.command.service.UserInfoCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserInfoKafkaConsumer {
    
    @Autowired
    private UserInfoCache userInfoCache;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "user-info-response", groupId = "order-service-group")
    public void handleUserInfoResponse(@Payload String message) {
        try {
            log.debug("Received user info response: {}", message);
            
            UserInfoUpdatedEvent event = objectMapper.readValue(message, UserInfoUpdatedEvent.class);
            
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(event.getUserId());
            userInfo.setUsername(event.getUsername());
            userInfo.setEmail(event.getEmail());
            userInfo.setRole(event.getRole());
            userInfo.setStatus(event.getStatus());
            
            userInfoCache.put(event.getUserId(), userInfo);
            log.debug("Updated user info cache for userId: {}", event.getUserId());
            
        } catch (Exception e) {
            log.error("Error processing user info response: {}", e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = "user-info-updated", groupId = "order-service-group")
    public void handleUserInfoUpdated(@Payload String message) {
        try {
            log.debug("Received user info updated event: {}", message);
            
            UserInfoUpdatedEvent event = objectMapper.readValue(message, UserInfoUpdatedEvent.class);
            
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(event.getUserId());
            userInfo.setUsername(event.getUsername());
            userInfo.setEmail(event.getEmail());
            userInfo.setRole(event.getRole());
            userInfo.setStatus(event.getStatus());
            
            userInfoCache.put(event.getUserId(), userInfo);
            log.debug("Updated user info cache from update event for userId: {}", event.getUserId());
            
        } catch (Exception e) {
            log.error("Error processing user info updated event: {}", e.getMessage(), e);
        }
    }
}

