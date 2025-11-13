package com.project3.inventoryservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.inventoryservice.command.entity.StockAlert;
import com.project3.inventoryservice.command.entity.StockAlertRepository;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/alerts")
@Tag(name = "Stock Alert Management", description = "APIs for managing stock alerts")
@Slf4j
public class StockAlertCommandController {
    
    @Autowired
    private StockAlertRepository stockAlertRepository;
    
    @PatchMapping("/{alertId}/resolve")
    @Operation(summary = "Đánh dấu cảnh báo đã được xử lý", description = "Đánh dấu một cảnh báo là đã được xử lý")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cảnh báo đã được đánh dấu xử lý"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cảnh báo")
    })
    public ResponseEntity<ApiResponseDTO<StockAlert>> resolveAlert(
            @Parameter(description = "ID của cảnh báo") @PathVariable String alertId,
            @Parameter(description = "Người xử lý") @RequestParam(required = false) String resolvedBy) {
        
        try {
            StockAlert alert = stockAlertRepository.findById(alertId)
                    .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
            
            alert.setIsActive(false);
            alert.setAcknowledgedAt(LocalDateTime.now());
            alert.setAcknowledgedBy(resolvedBy != null ? resolvedBy : "SYSTEM");
            
            StockAlert saved = stockAlertRepository.save(alert);
            
            return ResponseEntity.ok(ApiResponseDTO.success(saved, "Alert resolved successfully"));
        } catch (RuntimeException e) {
            log.error("Error resolving alert {}: {}", alertId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound(e.getMessage()));
        }
    }
    
    @PatchMapping("/{alertId}/dismiss")
    @Operation(summary = "Bỏ qua cảnh báo", description = "Đánh dấu một cảnh báo là đã bỏ qua")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cảnh báo đã được bỏ qua"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cảnh báo")
    })
    public ResponseEntity<ApiResponseDTO<StockAlert>> dismissAlert(
            @Parameter(description = "ID của cảnh báo") @PathVariable String alertId,
            @Parameter(description = "Người bỏ qua") @RequestParam(required = false) String dismissedBy) {
        
        try {
            StockAlert alert = stockAlertRepository.findById(alertId)
                    .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
            
            alert.setIsActive(false);
            alert.setAcknowledgedAt(LocalDateTime.now());
            alert.setAcknowledgedBy(dismissedBy != null ? dismissedBy : "SYSTEM");
            
            StockAlert saved = stockAlertRepository.save(alert);
            
            return ResponseEntity.ok(ApiResponseDTO.success(saved, "Alert dismissed successfully"));
        } catch (RuntimeException e) {
            log.error("Error dismissing alert {}: {}", alertId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound(e.getMessage()));
        }
    }
    
    @PostMapping("/bulk-resolve")
    @Operation(summary = "Đánh dấu nhiều cảnh báo đã được xử lý", description = "Đánh dấu nhiều cảnh báo là đã được xử lý")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Các cảnh báo đã được đánh dấu xử lý")
    })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> bulkResolveAlerts(
            @Parameter(description = "Danh sách ID cảnh báo") @RequestBody List<String> alertIds,
            @Parameter(description = "Người xử lý") @RequestParam(required = false) String resolvedBy) {
        
        int resolved = 0;
        int notFound = 0;
        
        for (String alertId : alertIds) {
            try {
                StockAlert alert = stockAlertRepository.findById(alertId)
                        .orElseThrow(() -> new RuntimeException("Alert not found"));
                
                alert.setIsActive(false);
                alert.setAcknowledgedAt(LocalDateTime.now());
                alert.setAcknowledgedBy(resolvedBy != null ? resolvedBy : "SYSTEM");
                
                stockAlertRepository.save(alert);
                resolved++;
            } catch (Exception e) {
                log.warn("Could not resolve alert {}: {}", alertId, e.getMessage());
                notFound++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("resolved", resolved);
        result.put("notFound", notFound);
        result.put("total", alertIds.size());
        
        return ResponseEntity.ok(ApiResponseDTO.success(
                result,
                String.format("Resolved %d out of %d alerts", resolved, alertIds.size())
        ));
    }
}

