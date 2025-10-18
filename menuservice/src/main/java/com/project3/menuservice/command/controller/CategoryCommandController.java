package com.project3.menuservice.command.controller;

import com.project3.menuservice.command.commands.*;
import com.project3.menuservice.util.IdGenerator;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant/category")
public class CategoryCommandController {

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public String create(@RequestBody CreateCategoryCommand cmd) {
        // Tự động sinh ID nếu không có hoặc rỗng
        if (cmd.getCategoryId() == null || cmd.getCategoryId().isEmpty()) {
            cmd.setCategoryId(IdGenerator.generateCategoryId());
        }
        return commandGateway.sendAndWait(cmd);
    }

    @PutMapping("/{id}")
    public String update(@PathVariable("id") String id, @RequestBody UpdateCategoryCommand cmd) {
        cmd.setCategoryId(id);
        return commandGateway.sendAndWait(cmd);
    }

    @PatchMapping("/{id}/active")
    public String toggleActive(@PathVariable("id") String id, @RequestParam("active") boolean active) {
        ToggleCategoryActiveCommand cmd = new ToggleCategoryActiveCommand(id, active);
        return commandGateway.sendAndWait(cmd);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") String id) {
        DeleteCategoryCommand cmd = new DeleteCategoryCommand(id);
        return commandGateway.sendAndWait(cmd);
    }
}
