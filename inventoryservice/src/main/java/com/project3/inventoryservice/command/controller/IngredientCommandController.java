package com.project3.inventoryservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.inventoryservice.command.commands.*;
import com.project3.inventoryservice.command.dto.BulkDeleteRequest;
import com.project3.inventoryservice.command.dto.BulkToggleRequest;
import com.project3.inventoryservice.util.IdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/ingredient")
@Tag(name = "Ingredient Commands", description = "APIs để quản lý nguyên liệu (Commands)")
@Slf4j
public class IngredientCommandController extends BaseInventoryController {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private com.project3.inventoryservice.command.service.BulkOperationService bulkOperationService;
    
    @PostMapping
    @Operation(summary = "Tạo nguyên liệu mới", description = "Tạo một nguyên liệu mới trong hệ thống")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    public ResponseEntity<ApiResponseDTO<String>> create(@RequestBody CreateIngredientCommand cmd) {
        try {
            if (cmd.getIngredientId() == null || cmd.getIngredientId().isEmpty()) {
                cmd.setIngredientId(IdGenerator.generateIngredientId());
            }
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.created(result, "Ingredient created successfully"));
        } catch (Exception e) {
            log.error("Error creating ingredient: {}", e.getMessage(), e);
            return badRequest("Failed to create ingredient: " + e.getMessage());
        }
    }
    
    @PutMapping("/{ingredientId}")
    @Operation(summary = "Cập nhật nguyên liệu", description = "Cập nhật thông tin của một nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<ApiResponseDTO<String>> update(
            @Parameter(description = "ID của nguyên liệu cần cập nhật") @PathVariable String ingredientId, 
            @RequestBody UpdateIngredientCommand cmd) {
        try {
            cmd.setIngredientId(ingredientId);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Ingredient updated successfully"));
        } catch (Exception e) {
            log.error("Error updating ingredient {}: {}", ingredientId, e.getMessage(), e);
            return badRequest("Failed to update ingredient: " + e.getMessage());
        }
    }

    @DeleteMapping("/{ingredientId}")
    @Operation(summary = "Xóa nguyên liệu", description = "Xóa một nguyên liệu khỏi hệ thống (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public ResponseEntity<ApiResponseDTO<String>> delete(
            @Parameter(description = "ID của nguyên liệu cần xóa") @PathVariable String ingredientId) {
        try {
            DeleteIngredientCommand cmd = new DeleteIngredientCommand(ingredientId);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Ingredient deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting ingredient {}: {}", ingredientId, e.getMessage(), e);
            return badRequest("Failed to delete ingredient: " + e.getMessage());
        }
    }

    @PutMapping("/{ingredientId}/toggle")
    @Operation(summary = "Bật/tắt nguyên liệu", description = "Thay đổi trạng thái hoạt động của nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thay đổi trạng thái thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public ResponseEntity<ApiResponseDTO<String>> toggleActive(
            @Parameter(description = "ID của nguyên liệu cần thay đổi trạng thái") @PathVariable String ingredientId) {
        try {
            ToggleIngredientActiveCommand cmd = new ToggleIngredientActiveCommand(ingredientId);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Ingredient status updated successfully"));
        } catch (Exception e) {
            log.error("Error toggling ingredient {} active status: {}", ingredientId, e.getMessage(), e);
            return badRequest("Failed to toggle ingredient status: " + e.getMessage());
        }
    }
    
    @PostMapping("/{ingredientId}/stock-in")
    @Operation(summary = "Nhập hàng", description = "Nhập hàng vào kho cho một nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nhập hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Số lượng không hợp lệ hoặc vượt quá giới hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public ResponseEntity<ApiResponseDTO<String>> stockIn(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId, 
            @RequestBody StockInCommand cmd) {
        try {
            cmd.setIngredientId(ingredientId);
            if (cmd.getTransactionId() == null || cmd.getTransactionId().isEmpty()) {
                cmd.setTransactionId(IdGenerator.generateStockInId());
            }
            if (cmd.getTransactionDate() == null) {
                cmd.setTransactionDate(LocalDateTime.now());
            }
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Stock in transaction completed successfully"));
        } catch (Exception e) {
            log.error("Error processing stock in for ingredient {}: {}", ingredientId, e.getMessage(), e);
            return badRequest("Failed to process stock in: " + e.getMessage());
        }
    }

    @PostMapping("/{ingredientId}/stock-out")
    @Operation(summary = "Xuất hàng", description = "Xuất hàng khỏi kho cho một nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xuất hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Không đủ tồn kho"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public ResponseEntity<ApiResponseDTO<String>> stockOut(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId, 
            @RequestBody StockOutCommand cmd) {
        try {
            cmd.setIngredientId(ingredientId);
            if (cmd.getTransactionId() == null || cmd.getTransactionId().isEmpty()) {
                cmd.setTransactionId(IdGenerator.generateStockOutId());
            }
            if (cmd.getTransactionDate() == null) {
                cmd.setTransactionDate(LocalDateTime.now());
            }
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Stock out transaction completed successfully"));
        } catch (Exception e) {
            log.error("Error processing stock out for ingredient {}: {}", ingredientId, e.getMessage(), e);
            return badRequest("Failed to process stock out: " + e.getMessage());
        }
    }

    @PostMapping("/{ingredientId}/adjust")
    @Operation(summary = "Điều chỉnh tồn kho", description = "Điều chỉnh số lượng tồn kho (tăng/giảm)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Điều chỉnh thành công"),
            @ApiResponse(responseCode = "400", description = "Điều chỉnh không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public ResponseEntity<ApiResponseDTO<String>> adjustStock(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId, 
            @RequestBody AdjustStockCommand cmd) {
        try {
            cmd.setIngredientId(ingredientId);
            if (cmd.getTransactionId() == null || cmd.getTransactionId().isEmpty()) {
                cmd.setTransactionId(IdGenerator.generateAdjustmentId());
            }
            if (cmd.getTransactionDate() == null) {
                cmd.setTransactionDate(LocalDateTime.now());
            }
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Stock adjustment completed successfully"));
        } catch (Exception e) {
            log.error("Error adjusting stock for ingredient {}: {}", ingredientId, e.getMessage(), e);
            return badRequest("Failed to adjust stock: " + e.getMessage());
        }
    }

    @PostMapping("/{ingredientId}/stock-take")
    @Operation(summary = "Kiểm kê", description = "Kiểm kê thực tế số lượng tồn kho")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kiểm kê thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu kiểm kê không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public ResponseEntity<ApiResponseDTO<String>> stockTake(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId, 
            @RequestBody StockTakeCommand cmd) {
        try {
            cmd.setIngredientId(ingredientId);
            if (cmd.getTransactionId() == null || cmd.getTransactionId().isEmpty()) {
                cmd.setTransactionId(IdGenerator.generateStockTakeId());
            }
            if (cmd.getTransactionDate() == null) {
                cmd.setTransactionDate(LocalDateTime.now());
            }
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Stock take completed successfully"));
        } catch (Exception e) {
            log.error("Error processing stock take for ingredient {}: {}", ingredientId, e.getMessage(), e);
            return badRequest("Failed to process stock take: " + e.getMessage());
        }
    }
    
    @PostMapping("/bulk-toggle")
    @Operation(summary = "Bật/tắt nhiều nguyên liệu", description = "Bật hoặc tắt nhiều nguyên liệu cùng lúc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thao tác thành công")
    })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> bulkToggle(@RequestBody BulkToggleRequest request) {
        try {
            Map<String, Object> result = bulkOperationService.executeBulkOperation(
                request.getIngredientIds(),
                id -> {
                    try {
                        ToggleIngredientActiveCommand cmd = new ToggleIngredientActiveCommand(id);
                        commandGateway.sendAndWait(cmd);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            );
            
            return ResponseEntity.ok(ApiResponseDTO.success(result, 
                    String.format("Successfully toggled %d ingredients, failed %d ingredients", 
                        result.get("success"), result.get("failed"))));
        } catch (Exception e) {
            log.error("Error bulk toggling ingredients: {}", e.getMessage(), e);
            return badRequest("Failed to bulk toggle ingredients: " + e.getMessage());
        }
    }

    @PostMapping("/bulk-delete")
    @Operation(summary = "Xóa nhiều nguyên liệu", description = "Xóa nhiều nguyên liệu cùng lúc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thao tác thành công")
    })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> bulkDelete(@RequestBody BulkDeleteRequest request) {
        try {
            Map<String, Object> result = bulkOperationService.executeBulkOperation(
                request.getIngredientIds(),
                id -> {
                    try {
                        DeleteIngredientCommand cmd = new DeleteIngredientCommand(id);
                        commandGateway.sendAndWait(cmd);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            );
            
            return ResponseEntity.ok(ApiResponseDTO.success(result, 
                    String.format("Successfully deleted %d ingredients, failed %d ingredients", 
                        result.get("success"), result.get("failed"))));
        } catch (Exception e) {
            log.error("Error bulk deleting ingredients: {}", e.getMessage(), e);
            return badRequest("Failed to bulk delete ingredients: " + e.getMessage());
        }
    }
}
