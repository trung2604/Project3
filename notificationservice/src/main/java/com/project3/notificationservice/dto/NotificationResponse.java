package com.project3.notificationservice.dto;

import com.project3.notificationservice.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private String notificationId;
    private String userId;
    private String type;
    private String title;
    private String message;
    private String severity;
    private String status;
    private String channel;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime archivedAt;
    private Boolean isActive;
    
    public static NotificationResponse fromEntity(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getNotificationId());
        response.setUserId(notification.getUserId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setSeverity(notification.getSeverity());
        response.setStatus(notification.getStatus());
        response.setChannel(notification.getChannel());
        response.setMetadata(notification.getMetadata());
        response.setCreatedAt(notification.getCreatedAt());
        response.setReadAt(notification.getReadAt());
        response.setArchivedAt(notification.getArchivedAt());
        response.setIsActive(notification.getIsActive());
        return response;
    }
}

