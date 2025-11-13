package com.project3.userservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.commonservice.dto.UserInfoUpdatedEvent;
import com.project3.commonservice.service.KafkaService;
import com.project3.userservice.dto.UserResponseDTO;
import com.project3.userservice.entity.User;
import com.project3.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class UserInfoKafkaConsumer {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private KafkaService kafkaService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "user-info-updated", groupId = "user-service-group")
    public void handleUserInfoUpdated(@Payload String message) {
        try {
            log.info("Received user info update event: {}", message);
            
            UserInfoUpdatedEvent event = objectMapper.readValue(message, UserInfoUpdatedEvent.class);
            
            User user = userRepository.findByUserId(event.getUserId()).orElse(null);
            if (user == null) {
                log.warn("User not found for userId: {}", event.getUserId());
                return;
            }
            
            UserResponseDTO userDTO = UserResponseDTO.fromEntity(user);
            UserInfoUpdatedEvent updatedEvent = new UserInfoUpdatedEvent();
            updatedEvent.setUserId(userDTO.getUserId());
            updatedEvent.setUsername(userDTO.getUsername());
            updatedEvent.setEmail(userDTO.getEmail());
            updatedEvent.setRole(userDTO.getRole() != null ? userDTO.getRole().name() : null);
            updatedEvent.setStatus(userDTO.getStatus() != null ? userDTO.getStatus().name() : null);
            updatedEvent.setTimestamp(System.currentTimeMillis());
            
            kafkaService.sendMessage("user-info-response", updatedEvent);
            log.info("Published user info response for userId: {}", event.getUserId());
            
        } catch (Exception e) {
            log.error("Error processing user info update event: {}", e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = "user-info-request", groupId = "user-service-group")
    public void handleUserInfoRequest(@Payload String message) {
        try {
            log.info("Received user info request: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> request = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            String userId = (String) request.get("userId");
            
            if (userId == null || userId.isEmpty()) {
                log.warn("Invalid user info request: missing userId");
                return;
            }
            
            User user = userRepository.findByUserId(userId).orElse(null);
            if (user == null) {
                log.warn("User not found for userId: {}", userId);
                return;
            }
            
            UserResponseDTO userDTO = UserResponseDTO.fromEntity(user);
            UserInfoUpdatedEvent event = new UserInfoUpdatedEvent();
            event.setUserId(userDTO.getUserId());
            event.setUsername(userDTO.getUsername());
            event.setEmail(userDTO.getEmail());
            event.setRole(userDTO.getRole() != null ? userDTO.getRole().name() : null);
            event.setStatus(userDTO.getStatus() != null ? userDTO.getStatus().name() : null);
            event.setTimestamp(System.currentTimeMillis());
            
            kafkaService.sendMessage("user-info-response", event);
            log.info("Published user info response for userId: {}", userId);
            
        } catch (Exception e) {
            log.error("Error processing user info request: {}", e.getMessage(), e);
        }
    }
}

