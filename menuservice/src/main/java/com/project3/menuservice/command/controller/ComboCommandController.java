package com.project3.menuservice.command.controller;

import com.project3.menuservice.command.commands.*;
import com.project3.menuservice.util.IdGenerator;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant/combo")
public class ComboCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public String create(@RequestBody CreateComboCommand cmd) {
        // Tự động sinh ID nếu không có hoặc rỗng
        if (cmd.getComboId() == null || cmd.getComboId().isEmpty()) {
            cmd.setComboId(IdGenerator.generateComboId());
        }
        return commandGateway.sendAndWait(cmd);
    }

    @PutMapping("/{id}")
    public String update(@PathVariable("id") String id, @RequestBody UpdateComboCommand cmd) {
        cmd.setComboId(id);
        return commandGateway.sendAndWait(cmd);
    }

    @PatchMapping("/{id}/active")
    public String toggleActive(@PathVariable("id") String id, @RequestParam("active") boolean active) {
        ToggleComboActiveCommand cmd = new ToggleComboActiveCommand(id, active);
        return commandGateway.sendAndWait(cmd);
    }

    @PostMapping("/{id}/items/{menuItemId}")
    public String addMenuItem(@PathVariable("id") String id, @PathVariable("menuItemId") String menuItemId) {
        AddMenuItemToComboCommand cmd = new AddMenuItemToComboCommand(id, menuItemId);
        return commandGateway.sendAndWait(cmd);
    }

    @DeleteMapping("/{id}/items/{menuItemId}")
    public String removeMenuItem(@PathVariable("id") String id, @PathVariable("menuItemId") String menuItemId) {
        RemoveMenuItemFromComboCommand cmd = new RemoveMenuItemFromComboCommand(id, menuItemId);
        return commandGateway.sendAndWait(cmd);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") String id) {
        DeleteComboCommand cmd = new DeleteComboCommand(id);
        return commandGateway.sendAndWait(cmd);
    }
}
