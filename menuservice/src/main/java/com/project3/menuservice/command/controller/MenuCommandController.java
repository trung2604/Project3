package com.project3.menuservice.command.controller;

import com.project3.menuservice.command.commands.*;
import com.project3.menuservice.util.IdGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant/menu")
@Tag(name = "Menu Item Commands", description = "APIs để quản lý món ăn (Commands)")
public class MenuCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping("/items")
    @Operation(summary = "Tạo món ăn mới", description = "Tạo một món ăn mới trong menu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    public String create(@RequestBody CreateMenuItemCommand cmd) {
        
        // Tự động sinh ID nếu không có hoặc rỗng
        if (cmd.getMenuItemId() == null || cmd.getMenuItemId().isEmpty()) {
            cmd.setMenuItemId(IdGenerator.generateMenuItemId());
        }
        return commandGateway.sendAndWait(cmd);
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Cập nhật món ăn", description = "Cập nhật thông tin của một món ăn")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy món ăn"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public String update(
            @Parameter(description = "ID của món ăn cần cập nhật") @PathVariable("id") String id, 
            @RequestBody UpdateMenuItemCommand cmd) {
        cmd.setMenuItemId(id);
        return commandGateway.sendAndWait(cmd);
    }

    @PatchMapping("/items/{id}/active")
    public String toggleActive(@PathVariable("id") String id, @RequestParam("active") boolean active) {
        ToggleMenuItemActiveCommand cmd = new ToggleMenuItemActiveCommand(id, active);
        return commandGateway.sendAndWait(cmd);
    }

    @PatchMapping("/items/{id}/ingredients")
    public String attachIngredients(@PathVariable("id") String id, @RequestBody List<String> ingredients) {
        AttachIngredientsCommand cmd = new AttachIngredientsCommand(id, ingredients);
        return commandGateway.sendAndWait(cmd);
    }

    @DeleteMapping("/items/{id}")
    public String delete(@PathVariable("id") String id) {
        DeleteMenuItemCommand cmd = new DeleteMenuItemCommand(id);
        return commandGateway.sendAndWait(cmd);
    }

    @PatchMapping("/items/{id}/auto-toggle")
    public String autoToggle(@PathVariable("id") String id, 
                            @RequestParam("reason") String reason,
                            @RequestParam("ingredientId") String ingredientId) {
        AutoToggleMenuItemCommand cmd = new AutoToggleMenuItemCommand(id, reason, ingredientId);
        return commandGateway.sendAndWait(cmd);
    }
}


