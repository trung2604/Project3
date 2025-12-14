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
@RequestMapping("/api/restaurant/combo")
@Slf4j
public class ComboCommandController extends BaseMenuController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<String>> create(@RequestBody CreateComboCommand cmd) {
        try {
            if (cmd.getComboId() == null || cmd.getComboId().isEmpty()) {
                cmd.setComboId(IdGenerator.generateComboId());
            }
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.created(result, "Combo created successfully"));
        } catch (Exception e) {
            log.error("Error creating combo: {}", e.getMessage(), e);
            return badRequest("Failed to create combo: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<String>> update(@PathVariable("id") String id, @RequestBody UpdateComboCommand cmd) {
        try {
            cmd.setComboId(id);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Combo updated successfully"));
        } catch (Exception e) {
            log.error("Error updating combo {}: {}", id, e.getMessage(), e);
            return badRequest("Failed to update combo: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponseDTO<String>> toggleActive(@PathVariable("id") String id, @RequestParam("active") boolean active) {
        try {
            ToggleComboActiveCommand cmd = new ToggleComboActiveCommand(id, active);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Combo status updated successfully"));
        } catch (Exception e) {
            log.error("Error toggling combo {} active status: {}", id, e.getMessage(), e);
            return badRequest("Failed to toggle combo status: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/items/{menuItemId}")
    public ResponseEntity<ApiResponseDTO<String>> addMenuItem(@PathVariable("id") String id, @PathVariable("menuItemId") String menuItemId) {
        try {
            AddMenuItemToComboCommand cmd = new AddMenuItemToComboCommand(id, menuItemId);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Menu item added to combo successfully"));
        } catch (Exception e) {
            log.error("Error adding menu item {} to combo {}: {}", menuItemId, id, e.getMessage(), e);
            return badRequest("Failed to add menu item to combo: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/items/{menuItemId}")
    public ResponseEntity<ApiResponseDTO<String>> removeMenuItem(@PathVariable("id") String id, @PathVariable("menuItemId") String menuItemId) {
        try {
            RemoveMenuItemFromComboCommand cmd = new RemoveMenuItemFromComboCommand(id, menuItemId);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Menu item removed from combo successfully"));
        } catch (Exception e) {
            log.error("Error removing menu item {} from combo {}: {}", menuItemId, id, e.getMessage(), e);
            return badRequest("Failed to remove menu item from combo: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<String>> delete(@PathVariable("id") String id) {
        try {
            DeleteComboCommand cmd = new DeleteComboCommand(id);
            String result = commandGateway.sendAndWait(cmd);
            return ResponseEntity.ok(ApiResponseDTO.success(result, "Combo deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting combo {}: {}", id, e.getMessage(), e);
            return badRequest("Failed to delete combo: " + e.getMessage());
        }
    }
}
