package com.project3.notificationservice.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.notificationservice.dto.NotificationResponse;
import com.project3.notificationservice.dto.PagedNotificationResponse;
import com.project3.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification Management", description = "APIs để quản lý thông báo")
@Slf4j
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo", description = "Lấy danh sách thông báo với các bộ lọc và phân trang")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    public ResponseEntity<ApiResponseDTO<PagedNotificationResponse>> getAll(
            @Parameter(description = "User ID") @RequestParam(required = false) String userId,
            @Parameter(description = "Trạng thái: UNREAD, READ, ARCHIVED") @RequestParam(required = false) String status,
            @Parameter(description = "Loại thông báo") @RequestParam(required = false) String type,
            @Parameter(description = "Mức độ: LOW, MEDIUM, HIGH, CRITICAL") @RequestParam(required = false) String severity,
            @Parameter(description = "Tìm kiếm") @RequestParam(required = false) String search,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "20") int size) {
        
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.badRequest("userId is required"));
        }
        
        PagedNotificationResponse response = notificationService.getNotifications(
            userId, status, type, severity, search, page, size);
        
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Notifications retrieved successfully"));
    }
    
    @GetMapping("/{notificationId}")
    @Operation(summary = "Lấy chi tiết thông báo", description = "Lấy thông tin chi tiết của một thông báo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông báo")
    })
    public ResponseEntity<ApiResponseDTO<NotificationResponse>> getById(
            @Parameter(description = "ID của thông báo") @PathVariable String notificationId) {
        
        try {
            NotificationResponse response = notificationService.getNotificationById(notificationId);
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Notification retrieved successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.notFound(e.getMessage()));
        }
    }
    
    @GetMapping("/unread")
    @Operation(summary = "Lấy thông báo chưa đọc", description = "Lấy danh sách thông báo chưa đọc của user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    public ResponseEntity<ApiResponseDTO<PagedNotificationResponse>> getUnread(
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "20") int size) {
        
        PagedNotificationResponse response = notificationService.getNotifications(
            userId, "UNREAD", null, null, null, page, size);
        
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Unread notifications retrieved successfully"));
    }
    
    @GetMapping("/unread/count")
    @Operation(summary = "Đếm số thông báo chưa đọc", description = "Đếm số lượng thông báo chưa đọc của user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đếm thành công")
    })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getUnreadCount(
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        Long count = notificationService.getUnreadCount(userId);
        
        Map<String, Object> result = Map.of("userId", userId, "unreadCount", count);
        
        return ResponseEntity.ok(ApiResponseDTO.success(result, "Unread count retrieved successfully"));
    }
    
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Đánh dấu đã đọc", description = "Đánh dấu một thông báo là đã đọc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đánh dấu thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông báo")
    })
    public ResponseEntity<ApiResponseDTO<NotificationResponse>> markAsRead(
            @Parameter(description = "ID của thông báo") @PathVariable String notificationId) {
        
        try {
            NotificationResponse response = notificationService.markAsRead(notificationId);
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Notification marked as read"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.notFound(e.getMessage()));
        }
    }
    
    @PatchMapping("/{notificationId}/archive")
    @Operation(summary = "Lưu vào archive", description = "Lưu một thông báo vào archive")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lưu thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông báo")
    })
    public ResponseEntity<ApiResponseDTO<NotificationResponse>> archive(
            @Parameter(description = "ID của thông báo") @PathVariable String notificationId) {
        
        try {
            NotificationResponse response = notificationService.archive(notificationId);
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Notification archived"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.notFound(e.getMessage()));
        }
    }
    
    @PostMapping("/bulk-read")
    @Operation(summary = "Đánh dấu nhiều thông báo đã đọc", description = "Đánh dấu nhiều thông báo là đã đọc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đánh dấu thành công")
    })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> bulkMarkAsRead(
            @Parameter(description = "Danh sách ID thông báo") @RequestBody List<String> notificationIds) {
        
        notificationService.bulkMarkAsRead(notificationIds);
        
        Map<String, Object> result = Map.of(
            "message", "Notifications marked as read",
            "count", notificationIds.size()
        );
        
        return ResponseEntity.ok(ApiResponseDTO.success(result, "Bulk mark as read successful"));
    }
    
    @PostMapping("/bulk-archive")
    @Operation(summary = "Lưu nhiều thông báo vào archive", description = "Lưu nhiều thông báo vào archive")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lưu thành công")
    })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> bulkArchive(
            @Parameter(description = "Danh sách ID thông báo") @RequestBody List<String> notificationIds) {
        
        notificationService.bulkArchive(notificationIds);
        
        Map<String, Object> result = Map.of(
            "message", "Notifications archived",
            "count", notificationIds.size()
        );
        
        return ResponseEntity.ok(ApiResponseDTO.success(result, "Bulk archive successful"));
    }
}

