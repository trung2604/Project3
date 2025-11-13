package com.project3.notificationservice.repository;

import com.project3.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    Page<Notification> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndStatusAndIsActiveTrueOrderByCreatedAtDesc(
        String userId, String status, Pageable pageable);
    
    Page<Notification> findByUserIdAndTypeAndIsActiveTrueOrderByCreatedAtDesc(
        String userId, String type, Pageable pageable);
    
    Page<Notification> findByUserIdAndSeverityAndIsActiveTrueOrderByCreatedAtDesc(
        String userId, String severity, Pageable pageable);
    
    @Query(value = "SELECT n.* FROM notifications n WHERE n.user_id = :userId AND n.is_active = true AND " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:type IS NULL OR n.type = :type) AND " +
           "(:severity IS NULL OR n.severity = :severity) AND " +
           "(:search IS NULL OR LOWER(CAST(n.title AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR " +
           "LOWER(CAST(n.message AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')) " +
           "ORDER BY n.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM notifications n WHERE n.user_id = :userId AND n.is_active = true AND " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:type IS NULL OR n.type = :type) AND " +
           "(:severity IS NULL OR n.severity = :severity) AND " +
           "(:search IS NULL OR LOWER(CAST(n.title AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR " +
           "LOWER(CAST(n.message AS TEXT)) LIKE LOWER('%' || CAST(:search AS TEXT) || '%'))",
           nativeQuery = true)
    Page<Notification> findByFilters(
        @Param("userId") String userId,
        @Param("status") String status,
        @Param("type") String type,
        @Param("severity") String severity,
        @Param("search") String search,
        Pageable pageable);
    
    Long countByUserIdAndStatusAndIsActiveTrue(String userId, String status);
    
    List<Notification> findByUserIdAndStatusAndIsActiveTrue(String userId, String status);
}

