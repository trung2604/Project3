package com.project3.notificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project3.notificationservice.client.UserServiceClient;
import com.project3.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OrderEventConsumer {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "order-created", groupId = "notification-service-group")
    public void handleOrderCreated(@Payload String message) {
        try {
            log.info("Received OrderCreatedEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String orderId = (String) event.get("orderId");
            String customerId = (String) event.get("customerId");
            Object totalAmountObj = event.get("totalAmount");
            String orderType = (String) event.get("orderType");
            
            Double totalAmount = totalAmountObj instanceof Number 
                ? ((Number) totalAmountObj).doubleValue() : null;
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderId", orderId);
            metadata.put("orderType", orderType);
            metadata.put("totalAmount", totalAmount);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Notify customer about order creation
            String title = "Đơn hàng mới đã được tạo";
            String notificationMessage = String.format(
                "Đơn hàng #%s của bạn đã được tạo thành công. Tổng tiền: %,.0f VNĐ", 
                orderId, totalAmount != null ? totalAmount : 0);
            
            if (customerId != null && !customerId.isEmpty()) {
                notificationService.createNotification(
                    customerId,
                    "ORDER_UPDATE",
                    title,
                    notificationMessage,
                    "LOW",
                    metadataJson
                );
                log.info("Created order created notification for customer {}", customerId);
            }
            
            // Notify kitchen staff about new order (if DINE_IN or DELIVERY)
            if ("DINE_IN".equals(orderType) || "DELIVERY".equals(orderType)) {
                List<String> kitchenStaffIds = userServiceClient.getUserIdsByRole("KITCHEN_STAFF");
                
                String kitchenTitle = "Đơn hàng mới cần chế biến";
                String kitchenMessage = String.format(
                    "Đơn hàng #%s mới (%s). Tổng tiền: %,.0f VNĐ", 
                    orderId, orderType, totalAmount != null ? totalAmount : 0);
                
                for (String staffId : kitchenStaffIds) {
                    notificationService.createNotification(
                        staffId,
                        "ORDER_UPDATE",
                        kitchenTitle,
                        kitchenMessage,
                        "HIGH",
                        metadataJson
                    );
                }
                log.info("Notified {} kitchen staff about new order {}", kitchenStaffIds.size(), orderId);
            }
            
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process OrderCreatedEvent", e);
        }
    }
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "order-status-updated", groupId = "notification-service-group")
    public void handleOrderStatusUpdated(@Payload String message) {
        try {
            log.info("Received OrderStatusUpdatedEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String orderId = (String) event.get("orderId");
            String customerId = (String) event.get("customerId");
            String previousStatus = (String) event.get("previousStatus");
            String newStatus = (String) event.get("newStatus");
            String orderType = (String) event.get("orderType");
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderId", orderId);
            metadata.put("previousStatus", previousStatus);
            metadata.put("newStatus", newStatus);
            metadata.put("orderType", orderType);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Determine notification details based on status
            String title = "";
            String notificationMessage = "";
            String severity = "MEDIUM";
            
            switch (newStatus) {
                case "PENDING":
                    title = "Đơn hàng đang chờ xử lý";
                    notificationMessage = String.format("Đơn hàng #%s đang chờ xử lý", orderId);
                    break;
                case "COOKING":
                    title = "Đơn hàng đang được chế biến";
                    notificationMessage = String.format("Đơn hàng #%s đang được chế biến", orderId);
                    severity = "MEDIUM";
                    break;
                case "READY":
                    title = "Đơn hàng đã sẵn sàng";
                    notificationMessage = String.format("Đơn hàng #%s đã sẵn sàng. Vui lòng đến nhận hoặc đợi giao hàng", orderId);
                    severity = "HIGH";
                    break;
                case "DELIVERING":
                    title = "Đơn hàng đang được giao";
                    notificationMessage = String.format("Đơn hàng #%s đang được giao đến bạn", orderId);
                    severity = "HIGH";
                    break;
                case "COMPLETED":
                    title = "Đơn hàng đã hoàn thành";
                    notificationMessage = String.format("Đơn hàng #%s đã được hoàn thành. Cảm ơn bạn đã sử dụng dịch vụ!", orderId);
                    severity = "LOW";
                    break;
                case "CANCELLED":
                    title = "Đơn hàng đã bị hủy";
                    notificationMessage = String.format("Đơn hàng #%s đã bị hủy", orderId);
                    severity = "MEDIUM";
                    break;
                default:
                    title = "Cập nhật trạng thái đơn hàng";
                    notificationMessage = String.format("Đơn hàng #%s đã được cập nhật: %s", orderId, newStatus);
            }
            
            // Notify customer
            if (customerId != null && !customerId.isEmpty()) {
                notificationService.createNotification(
                    customerId,
                    "ORDER_UPDATE",
                    title,
                    notificationMessage,
                    severity,
                    metadataJson
                );
                log.info("Created order status update notification for customer {}: {} -> {}", 
                    customerId, previousStatus, newStatus);
            }
            
            // Notify kitchen staff when order status changes to COOKING
            if ("COOKING".equals(newStatus)) {
                List<String> kitchenStaffIds = userServiceClient.getUserIdsByRole("KITCHEN_STAFF");
                
                String kitchenTitle = "Đơn hàng đang được chế biến";
                String kitchenMessage = String.format("Đơn hàng #%s đang được chế biến", orderId);
                
                for (String staffId : kitchenStaffIds) {
                    notificationService.createNotification(
                        staffId,
                        "ORDER_UPDATE",
                        kitchenTitle,
                        kitchenMessage,
                        "MEDIUM",
                        metadataJson
                    );
                }
                log.info("Notified {} kitchen staff about order {} status: {}", 
                    kitchenStaffIds.size(), orderId, newStatus);
            }
            
            // Notify staff when order is READY (for pickup/delivery)
            if ("READY".equals(newStatus)) {
                List<String> staffIds = userServiceClient.getUserIdsByRoles("STAFF", "RESTAURANT_MANAGER");
                
                String staffTitle = "Đơn hàng đã sẵn sàng";
                String staffMessage = String.format("Đơn hàng #%s đã sẵn sàng để phục vụ/giao hàng", orderId);
                
                for (String staffId : staffIds) {
                    notificationService.createNotification(
                        staffId,
                        "ORDER_UPDATE",
                        staffTitle,
                        staffMessage,
                        "HIGH",
                        metadataJson
                    );
                }
                log.info("Notified {} staff about order {} ready", staffIds.size(), orderId);
            }
            
        } catch (Exception e) {
            log.error("Error processing OrderStatusUpdatedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process OrderStatusUpdatedEvent", e);
        }
    }
    
    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        autoCreateTopics = "true",
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "order-cancelled", groupId = "notification-service-group")
    public void handleOrderCancelled(@Payload String message) {
        try {
            log.info("Received OrderCancelledEvent: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);
            
            String orderId = (String) event.get("orderId");
            String customerId = (String) event.get("customerId");
            String cancellationReason = (String) event.get("cancellationReason");
            Object totalAmountObj = event.get("totalAmount");
            
            Double totalAmount = totalAmountObj instanceof Number 
                ? ((Number) totalAmountObj).doubleValue() : null;
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderId", orderId);
            metadata.put("cancellationReason", cancellationReason);
            metadata.put("totalAmount", totalAmount);
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Notify customer
            String title = "Đơn hàng đã bị hủy";
            String notificationMessage = String.format(
                "Đơn hàng #%s đã bị hủy. Lý do: %s", 
                orderId, 
                cancellationReason != null ? cancellationReason : "Không xác định");
            
            if (customerId != null && !customerId.isEmpty()) {
                notificationService.createNotification(
                    customerId,
                    "ORDER_UPDATE",
                    title,
                    notificationMessage,
                    "MEDIUM",
                    metadataJson
                );
                log.info("Created order cancelled notification for customer {}", customerId);
            }
            
            // Notify managers/admins about order cancellation
            List<String> managerIds = userServiceClient.getUserIdsByRoles("RESTAURANT_MANAGER", "ADMIN");
            
            String managerTitle = "Đơn hàng đã bị hủy";
            String managerMessage = String.format(
                "Đơn hàng #%s đã bị hủy. Khách hàng: %s. Lý do: %s", 
                orderId,
                customerId != null ? customerId : "N/A",
                cancellationReason != null ? cancellationReason : "Không xác định");
            
            for (String managerId : managerIds) {
                notificationService.createNotification(
                    managerId,
                    "ORDER_UPDATE",
                    managerTitle,
                    managerMessage,
                    "MEDIUM",
                    metadataJson
                );
            }
            log.info("Notified {} managers/admins about order cancellation {}", managerIds.size(), orderId);
            
        } catch (Exception e) {
            log.error("Error processing OrderCancelledEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process OrderCancelledEvent", e);
        }
    }
}

