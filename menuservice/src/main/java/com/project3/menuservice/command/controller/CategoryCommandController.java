package com.project3.menuservice.command.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.menuservice.command.commands.*;
import com.project3.menuservice.command.entity.Category;
import com.project3.menuservice.command.entity.CategoryRepository;
import com.project3.menuservice.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant/category")
@Slf4j
public class CategoryCommandController extends BaseMenuController {

    @Autowired
    private CommandGateway commandGateway;
    
    @Autowired
    private CategoryRepository categoryRepository;

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
            return badRequest("Failed to create category: " + e.getMessage());
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
            return badRequest("Failed to update category: " + e.getMessage());
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
            return badRequest("Failed to toggle category status: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<String>> delete(@PathVariable("id") String id) {
        try {
            DeleteCategoryCommand cmd = new DeleteCategoryCommand(id);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Category deleted successfully"));
        } catch (AggregateNotFoundException e) {
            log.warn("Aggregate not found in event store for category {}, attempting to rebuild from read model: {}", id, e.getMessage());
            return handleAggregateNotFoundForDelete(id);
        } catch (Exception e) {
            // Check if the exception or its cause contains "aggregate" and "not found" in the message
            if (isAggregateNotFound(e)) {
                String errorMessage = e.getMessage();
                log.warn("Aggregate not found in event store for category {} (wrapped exception), attempting to rebuild: {}", id, errorMessage);
                return handleAggregateNotFoundForDelete(id);
            }
            log.error("Error deleting category {}: {}", id, e.getMessage(), e);
            return badRequest("Failed to delete category: " + e.getMessage());
        }
    }
    
    private ResponseEntity<ApiResponseDTO<String>> handleAggregateNotFoundForDelete(String id) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.error("Category not found: " + id, 404));
        }
        
        try {

            log.info("Rebuilding category aggregate {} from read model before deletion", id);
            CreateCategoryCommand createCmd = new CreateCategoryCommand();
            createCmd.setCategoryId(category.getCategoryId());
            createCmd.setName(category.getName());
            createCmd.setDescription(category.getDescription());
            createCmd.setType(category.getType());
            createCmd.setActive(category.getActive());
            
            commandGateway.sendAndWait(createCmd);
            log.info("Successfully rebuilt category aggregate {} from read model", id);

            DeleteCategoryCommand deleteCmd = new DeleteCategoryCommand(id);
            String result = commandGateway.sendAndWait(deleteCmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Category deleted successfully (rebuilt from read model first)"));
        } catch (Exception rebuildError) {
            log.error("Error rebuilding category aggregate {} from read model: {}", id, rebuildError.getMessage(), rebuildError);
            try {
                categoryRepository.deleteById(id);
                log.warn("Deleted category {} directly from read model as fallback", id);
                return ResponseEntity.ok(ApiResponseDTO.success(id, "Category deleted from read model (aggregate rebuild failed)"));
            } catch (Exception deleteError) {
                log.error("Error deleting category {} from read model: {}", id, deleteError.getMessage(), deleteError);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponseDTO.error("Failed to delete category: " + deleteError.getMessage(), 500));
            }
        }
    }
}
