package com.project3.inventoryservice.command.controller;

import com.project3.inventoryservice.command.commands.*;
import com.project3.inventoryservice.util.IdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/inventory/ingredient")
@Tag(name = "Ingredient Commands", description = "APIs để quản lý nguyên liệu (Commands)")
public class IngredientCommandController {
    
    @Autowired
    private CommandGateway commandGateway;
    
    @PostMapping
    @Operation(summary = "Tạo nguyên liệu mới", description = "Tạo một nguyên liệu mới trong hệ thống")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    public String create(@RequestBody CreateIngredientCommand cmd) {
        if (cmd.getIngredientId() == null || cmd.getIngredientId().isEmpty()) {
            cmd.setIngredientId(IdGenerator.generateIngredientId());
        }
        return commandGateway.sendAndWait(cmd);
    }
    
    @PutMapping("/{ingredientId}")
    @Operation(summary = "Cập nhật nguyên liệu", description = "Cập nhật thông tin của một nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public String update(
            @Parameter(description = "ID của nguyên liệu cần cập nhật") @PathVariable String ingredientId, 
            @RequestBody UpdateIngredientCommand cmd) {
        cmd.setIngredientId(ingredientId);
        return commandGateway.sendAndWait(cmd);
    }
    
    @DeleteMapping("/{ingredientId}")
    @Operation(summary = "Xóa nguyên liệu", description = "Xóa một nguyên liệu khỏi hệ thống (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public String delete(
            @Parameter(description = "ID của nguyên liệu cần xóa") @PathVariable String ingredientId) {
        DeleteIngredientCommand cmd = new DeleteIngredientCommand(ingredientId);
        return commandGateway.sendAndWait(cmd);
    }
    
    @PutMapping("/{ingredientId}/toggle")
    @Operation(summary = "Bật/tắt nguyên liệu", description = "Thay đổi trạng thái hoạt động của nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thay đổi trạng thái thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public String toggleActive(
            @Parameter(description = "ID của nguyên liệu cần thay đổi trạng thái") @PathVariable String ingredientId) {
        ToggleIngredientActiveCommand cmd = new ToggleIngredientActiveCommand(ingredientId);
        return commandGateway.sendAndWait(cmd);
    }
    
    @PostMapping("/{ingredientId}/stock-in")
    @Operation(summary = "Nhập hàng", description = "Nhập hàng vào kho cho một nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nhập hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Số lượng không hợp lệ hoặc vượt quá giới hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public String stockIn(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId, 
            @RequestBody StockInCommand cmd) {
        cmd.setIngredientId(ingredientId);
        if (cmd.getTransactionId() == null || cmd.getTransactionId().isEmpty()) {
            cmd.setTransactionId(IdGenerator.generateStockInId());
        }
        if (cmd.getTransactionDate() == null) {
            cmd.setTransactionDate(LocalDateTime.now());
        }
        return commandGateway.sendAndWait(cmd);
    }
    
    @PostMapping("/{ingredientId}/stock-out")
    @Operation(summary = "Xuất hàng", description = "Xuất hàng khỏi kho cho một nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xuất hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Không đủ tồn kho"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public String stockOut(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId, 
            @RequestBody StockOutCommand cmd) {
        cmd.setIngredientId(ingredientId);
        if (cmd.getTransactionId() == null || cmd.getTransactionId().isEmpty()) {
            cmd.setTransactionId(IdGenerator.generateStockOutId());
        }
        if (cmd.getTransactionDate() == null) {
            cmd.setTransactionDate(LocalDateTime.now());
        }
        return commandGateway.sendAndWait(cmd);
    }
    
    @PostMapping("/{ingredientId}/adjust")
    @Operation(summary = "Điều chỉnh tồn kho", description = "Điều chỉnh số lượng tồn kho (tăng/giảm)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Điều chỉnh thành công"),
            @ApiResponse(responseCode = "400", description = "Điều chỉnh không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public String adjustStock(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId, 
            @RequestBody AdjustStockCommand cmd) {
        cmd.setIngredientId(ingredientId);
        if (cmd.getTransactionId() == null || cmd.getTransactionId().isEmpty()) {
            cmd.setTransactionId(IdGenerator.generateAdjustmentId());
        }
        if (cmd.getTransactionDate() == null) {
            cmd.setTransactionDate(LocalDateTime.now());
        }
        return commandGateway.sendAndWait(cmd);
    }
    
    @PostMapping("/{ingredientId}/stock-take")
    @Operation(summary = "Kiểm kê", description = "Kiểm kê thực tế số lượng tồn kho")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kiểm kê thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu kiểm kê không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public String stockTake(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId, 
            @RequestBody StockTakeCommand cmd) {
        cmd.setIngredientId(ingredientId);
        if (cmd.getTransactionId() == null || cmd.getTransactionId().isEmpty()) {
            cmd.setTransactionId(IdGenerator.generateStockTakeId());
        }
        if (cmd.getTransactionDate() == null) {
            cmd.setTransactionDate(LocalDateTime.now());
        }
        return commandGateway.sendAndWait(cmd);
    }
}
