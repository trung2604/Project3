package com.project3.notificationservice.service;

import com.project3.notificationservice.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications via WebSocket
 */
@Service
@Slf4j
public class WebSocketNotificationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Send notification to a specific user via WebSocket
     * @param userId The user ID to send notification to
     * @param notification The notification response
     */
    public void sendNotificationToUser(String userId, NotificationResponse notification) {
        try {
            // Send to user-specific destination: /user/{userId}/notifications
            String destination = "/user/" + userId + "/notifications";
            messagingTemplate.convertAndSend(destination, notification);
            log.info("Sent WebSocket notification to user {}: {}", userId, notification.getNotificationId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Send notification count update to a specific user
     * @param userId The user ID
     * @param unreadCount The unread notification count
     */
    public void sendUnreadCountToUser(String userId, Long unreadCount) {
        try {
            String destination = "/user/" + userId + "/notifications/count";
            messagingTemplate.convertAndSend(destination, unreadCount);
            log.debug("Sent unread count to user {}: {}", userId, unreadCount);
        } catch (Exception e) {
            log.error("Failed to send unread count to user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Send notification to all users subscribed to a topic (for broadcast notifications)
     * @param topic The topic destination (e.g., "/topic/admin/announcements")
     * @param notification The notification response
     */
    public void sendNotificationToTopic(String topic, NotificationResponse notification) {
        try {
            messagingTemplate.convertAndSend(topic, notification);
            log.info("Sent WebSocket notification to topic {}: {}", topic, notification.getNotificationId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to topic {}: {}", topic, e.getMessage(), e);
        }
    }
}

