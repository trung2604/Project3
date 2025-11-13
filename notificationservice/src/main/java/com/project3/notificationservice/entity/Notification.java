package com.project3.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user_id", columnList = "user_id"),
    @Index(name = "idx_notifications_status", columnList = "status"),
    @Index(name = "idx_notifications_type", columnList = "type"),
    @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    
    @Id
    @Column(name = "notification_id")
    private String notificationId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "type", nullable = false, length = 50)
    private String type; // INVENTORY_ALERT, ORDER_UPDATE, SYSTEM, etc.
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "severity", length = 20)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "status", length = 20, nullable = false)
    private String status = "UNREAD"; // UNREAD, READ, ARCHIVED
    
    @Column(name = "channel", length = 20)
    private String channel = "IN_APP"; // IN_APP, EMAIL, SMS, PUSH
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "UNREAD";
        }
        if (channel == null) {
            channel = "IN_APP";
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}

