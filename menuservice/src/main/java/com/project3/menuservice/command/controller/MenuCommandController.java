package com.project3.menuservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.menuservice.command.commands.*;
import com.project3.menuservice.command.dto.BulkDeleteRequest;
import com.project3.menuservice.command.dto.BulkToggleRequest;
import com.project3.menuservice.command.entity.MenuItem;
import com.project3.menuservice.command.entity.MenuItemRepository;
import com.project3.menuservice.util.IdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurant/menu")
@Tag(name = "Menu Item Commands", description = "APIs để quản lý món ăn (Commands)")
@Slf4j
public class MenuCommandController {

    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private MenuItemRepository menuItemRepository;

    @PostMapping("/items")
    @Operation(summary = "Tạo món ăn mới", description = "Tạo một món ăn mới trong menu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    public ResponseEntity<ApiResponseDTO<String>> create(@RequestBody CreateMenuItemCommand cmd) {
        try {
            if (cmd.getMenuItemId() == null || cmd.getMenuItemId().isEmpty()) {
                cmd.setMenuItemId(IdGenerator.generateMenuItemId());
            }
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.created(result, "Menu item created successfully"));
        } catch (Exception e) {
            log.error("Error creating menu item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to create menu item: " + e.getMessage(), 400));
        }
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Cập nhật món ăn", description = "Cập nhật thông tin của một món ăn")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy món ăn"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<ApiResponseDTO<String>> update(
            @Parameter(description = "ID của món ăn cần cập nhật") @PathVariable("id") String id, 
            @RequestBody UpdateMenuItemCommand cmd) {
        try {
            cmd.setMenuItemId(id);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Menu item updated successfully"));
        } catch (Exception e) {
            log.error("Error updating menu item {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to update menu item: " + e.getMessage(), 400));
        }
    }

    @PatchMapping("/items/{id}/active")
    public ResponseEntity<ApiResponseDTO<String>> toggleActive(@PathVariable("id") String id, @RequestParam("active") boolean active) {
        try {
            MenuItem menuItem = menuItemRepository.findById(id).orElse(null);
            if (menuItem == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.error("Menu item not found: " + id, 404));
            }
            
            ToggleMenuItemActiveCommand cmd = new ToggleMenuItemActiveCommand(id, active);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Menu item status updated successfully"));
        } catch (AggregateNotFoundException e) {
            log.warn("Aggregate not found in event store for menu item {}, syncing with read model: {}", id, e.getMessage());
            return handleAggregateNotFoundFallback(id, () -> {
                MenuItem menuItem = menuItemRepository.findById(id).orElse(null);
                if (menuItem != null) {
                    menuItem.setActive(active);
                    menuItemRepository.save(menuItem);
                    return ResponseEntity.ok(ApiResponseDTO.success(id, "Menu item status updated successfully (synced with read model)"));
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.error("Menu item not found: " + id, 404));
            });
        } catch (Exception e) {
            // Check if the exception or its cause contains "aggregate" and "not found" in the message
            String errorMessage = e.getMessage();
            Throwable cause = e.getCause();
            String causeMessage = cause != null ? cause.getMessage() : null;
            
            boolean isAggregateNotFound = (errorMessage != null && 
                (errorMessage.toLowerCase().contains("aggregate") && errorMessage.toLowerCase().contains("not found"))) ||
                (causeMessage != null && 
                (causeMessage.toLowerCase().contains("aggregate") && causeMessage.toLowerCase().contains("not found")));
            
            if (isAggregateNotFound) {
                log.warn("Aggregate not found in event store for menu item {} (wrapped exception), syncing with read model. Error: {}", id, errorMessage);
                return handleAggregateNotFoundFallback(id, () -> {
                    MenuItem menuItem = menuItemRepository.findById(id).orElse(null);
                    if (menuItem != null) {
                        menuItem.setActive(active);
                        menuItemRepository.save(menuItem);
                        return ResponseEntity.ok(ApiResponseDTO.success(id, "Menu item status updated successfully (synced with read model)"));
                    }
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponseDTO.error("Menu item not found: " + id, 404));
                });
            }
            log.error("Error toggling menu item {} active status: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to toggle menu item status: " + e.getMessage(), 400));
        }
    }
    
    private ResponseEntity<ApiResponseDTO<String>> handleAggregateNotFoundFallback(String id, java.util.function.Supplier<ResponseEntity<ApiResponseDTO<String>>> fallback) {
        try {
            return fallback.get();
        } catch (Exception e) {
            log.error("Error in fallback handler for menu item {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to sync menu item: " + e.getMessage(), 500));
        }
    }

    @PatchMapping("/items/{id}/ingredients")
    public ResponseEntity<ApiResponseDTO<String>> attachIngredients(@PathVariable("id") String id, @RequestBody List<String> ingredients) {
        try {
            MenuItem menuItem = menuItemRepository.findById(id).orElse(null);
            if (menuItem == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.error("Menu item not found: " + id, 404));
            }
            
            AttachIngredientsCommand cmd = new AttachIngredientsCommand(id, ingredients);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Ingredients attached successfully"));
        } catch (AggregateNotFoundException e) {
            log.warn("Aggregate not found in event store for menu item {}, syncing with read model: {}", id, e.getMessage());
            return handleAggregateNotFoundFallback(id, () -> {
                MenuItem menuItem = menuItemRepository.findById(id).orElse(null);
                if (menuItem != null) {
                    menuItem.setIngredients(ingredients);
                    menuItemRepository.save(menuItem);
                    return ResponseEntity.ok(ApiResponseDTO.success(id, "Ingredients attached successfully (synced with read model)"));
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.error("Menu item not found: " + id, 404));
            });
        } catch (Exception e) {
            // Check if the exception or its cause contains "aggregate" and "not found" in the message
            String errorMessage = e.getMessage();
            Throwable cause = e.getCause();
            String causeMessage = cause != null ? cause.getMessage() : null;
            
            boolean isAggregateNotFound = (errorMessage != null && 
                (errorMessage.toLowerCase().contains("aggregate") && errorMessage.toLowerCase().contains("not found"))) ||
                (causeMessage != null && 
                (causeMessage.toLowerCase().contains("aggregate") && causeMessage.toLowerCase().contains("not found")));
            
            if (isAggregateNotFound) {
                log.warn("Aggregate not found in event store for menu item {} (wrapped exception), syncing with read model. Error: {}", id, errorMessage);
                return handleAggregateNotFoundFallback(id, () -> {
                    MenuItem menuItem = menuItemRepository.findById(id).orElse(null);
                    if (menuItem != null) {
                        menuItem.setIngredients(ingredients);
                        menuItemRepository.save(menuItem);
                        return ResponseEntity.ok(ApiResponseDTO.success(id, "Ingredients attached successfully (synced with read model)"));
                    }
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponseDTO.error("Menu item not found: " + id, 404));
                });
            }
            log.error("Error attaching ingredients to menu item {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to attach ingredients: " + e.getMessage(), 400));
        }
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponseDTO<String>> delete(@PathVariable("id") String id) {
        try {
            DeleteMenuItemCommand cmd = new DeleteMenuItemCommand(id);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Menu item deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting menu item {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to delete menu item: " + e.getMessage(), 400));
        }
    }

    @PatchMapping("/items/{id}/auto-toggle")
    public ResponseEntity<ApiResponseDTO<String>> autoToggle(@PathVariable("id") String id, 
                            @RequestParam("reason") String reason,
                            @RequestParam("ingredientId") String ingredientId) {
        try {
            AutoToggleMenuItemCommand cmd = new AutoToggleMenuItemCommand(id, reason, ingredientId);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Menu item auto-toggled successfully"));
        } catch (Exception e) {
            log.error("Error auto-toggling menu item {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to auto-toggle menu item: " + e.getMessage(), 400));
        }
    }
    
    @PostMapping("/items/bulk-toggle")
    @Operation(summary = "Bật/tắt nhiều món ăn", description = "Bật hoặc tắt nhiều món ăn cùng lúc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thao tác thành công")
    })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> bulkToggle(@RequestBody BulkToggleRequest request) {
        try {
            int success = 0;
            int failed = 0;
            
            for (String id : request.getMenuItemIds()) {
                try {
                    ToggleMenuItemActiveCommand cmd = new ToggleMenuItemActiveCommand(id, request.getActive());
                    commandGateway.sendAndWait(cmd);
                    success++;
                } catch (Exception e) {
                    failed++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("failed", failed);
            result.put("total", request.getMenuItemIds().size());
            
            return ResponseEntity.ok(ApiResponseDTO.success(result, 
                    String.format("Successfully toggled %d items, failed %d items", success, failed)));
        } catch (Exception e) {
            log.error("Error bulk toggling menu items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to bulk toggle menu items: " + e.getMessage(), 400));
        }
    }
    
    @PostMapping("/items/bulk-delete")
    @Operation(summary = "Xóa nhiều món ăn", description = "Xóa nhiều món ăn cùng lúc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thao tác thành công")
    })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> bulkDelete(@RequestBody BulkDeleteRequest request) {
        try {
            int success = 0;
            int failed = 0;
            
            for (String id : request.getMenuItemIds()) {
                try {
                    DeleteMenuItemCommand cmd = new DeleteMenuItemCommand(id);
                    commandGateway.sendAndWait(cmd);
                    success++;
                } catch (Exception e) {
                    failed++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("failed", failed);
            result.put("total", request.getMenuItemIds().size());
            
            return ResponseEntity.ok(ApiResponseDTO.success(result, 
                    String.format("Successfully deleted %d items, failed %d items", success, failed)));
        } catch (Exception e) {
            log.error("Error bulk deleting menu items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to bulk delete menu items: " + e.getMessage(), 400));
        }
    }
}
