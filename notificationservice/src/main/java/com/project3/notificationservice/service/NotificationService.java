package com.project3.notificationservice.service;

import com.project3.notificationservice.dto.NotificationResponse;
import com.project3.notificationservice.dto.PagedNotificationResponse;
import com.project3.notificationservice.entity.Notification;
import com.project3.notificationservice.repository.NotificationRepository;
import com.project3.notificationservice.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;
    
    public Notification createNotification(String userId, String type, String title, 
                                          String message, String severity, String metadata) {
        Notification notification = new Notification();
        notification.setNotificationId(IdGenerator.generateNotificationId());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSeverity(severity);
        notification.setStatus("UNREAD");
        notification.setChannel("IN_APP");
        notification.setMetadata(metadata);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsActive(true);
        
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification {} for user {}", saved.getNotificationId(), userId);
        
        // Send notification via WebSocket
        try {
            NotificationResponse response = NotificationResponse.fromEntity(saved);
            webSocketNotificationService.sendNotificationToUser(userId, response);
            
            // Also send unread count update
            Long unreadCount = getUnreadCount(userId);
            webSocketNotificationService.sendUnreadCountToUser(userId, unreadCount);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification for user {}: {}", userId, e.getMessage());
            // Don't fail the notification creation if WebSocket fails
        }
        
        return saved;
    }
    
    public PagedNotificationResponse getNotifications(String userId, String status, 
                                                      String type, String severity, 
                                                      String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository.findByFilters(
            userId, status, type, severity, search, pageable);
        
        List<NotificationResponse> notifications = notificationPage.getContent().stream()
            .map(NotificationResponse::fromEntity)
            .collect(Collectors.toList());
        
        return new PagedNotificationResponse(
            notifications,
            notificationPage.getNumber(),
            notificationPage.getSize(),
            notificationPage.getTotalElements(),
            notificationPage.getTotalPages(),
            notificationPage.hasNext(),
            notificationPage.hasPrevious()
        );
    }
    
    public NotificationResponse getNotificationById(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        return NotificationResponse.fromEntity(notification);
    }
    
    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        notification.setStatus("READ");
        notification.setReadAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        log.info("Marked notification {} as read", notificationId);
        return NotificationResponse.fromEntity(saved);
    }
    
    @Transactional
    public NotificationResponse archive(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        notification.setStatus("ARCHIVED");
        notification.setArchivedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        log.info("Archived notification {}", notificationId);
        return NotificationResponse.fromEntity(saved);
    }
    
    public Long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndStatusAndIsActiveTrue(userId, "UNREAD");
    }
    
    @Transactional
    public void bulkMarkAsRead(List<String> notificationIds) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        LocalDateTime now = LocalDateTime.now();
        
        for (Notification notification : notifications) {
            notification.setStatus("READ");
            notification.setReadAt(now);
        }
        
        notificationRepository.saveAll(notifications);
        log.info("Bulk marked {} notifications as read", notifications.size());
    }
    
    @Transactional
    public void bulkArchive(List<String> notificationIds) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        LocalDateTime now = LocalDateTime.now();
        
        for (Notification notification : notifications) {
            notification.setStatus("ARCHIVED");
            notification.setArchivedAt(now);
        }
        
        notificationRepository.saveAll(notifications);
        log.info("Bulk archived {} notifications", notifications.size());
    }
}

