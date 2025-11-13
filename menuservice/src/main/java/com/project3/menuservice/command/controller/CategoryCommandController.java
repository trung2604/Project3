package com.project3.menuservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.menuservice.command.commands.*;
import com.project3.menuservice.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant/category")
@Slf4j
public class CategoryCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<String>> create(@RequestBody CreateCategoryCommand cmd) {
        try {
            if (cmd.getCategoryId() == null || cmd.getCategoryId().isEmpty()) {
                cmd.setCategoryId(IdGenerator.generateCategoryId());
            }
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.created(result, "Category created successfully"));
        } catch (Exception e) {
            log.error("Error creating category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to create category: " + e.getMessage(), 400));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<String>> update(@PathVariable("id") String id, @RequestBody UpdateCategoryCommand cmd) {
        try {
            cmd.setCategoryId(id);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Category updated successfully"));
        } catch (Exception e) {
            log.error("Error updating category {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to update category: " + e.getMessage(), 400));
        }
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponseDTO<String>> toggleActive(@PathVariable("id") String id, @RequestParam("active") boolean active) {
        try {
            ToggleCategoryActiveCommand cmd = new ToggleCategoryActiveCommand(id, active);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Category status updated successfully"));
        } catch (Exception e) {
            log.error("Error toggling category {} active status: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to toggle category status: " + e.getMessage(), 400));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<String>> delete(@PathVariable("id") String id) {
        try {
            DeleteCategoryCommand cmd = new DeleteCategoryCommand(id);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Category deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting category {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.error("Failed to delete category: " + e.getMessage(), 400));
        }
    }
}
