package com.project3.menuservice.command.aggregate;

import com.project3.menuservice.command.commands.*;
import com.project3.menuservice.command.event.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Aggregate
public class MenuItemAggregate {

    @AggregateIdentifier
    private String menuItemId;
    private String name;
    private String categoryId; // Changed from category string to categoryId
    private String description;
    private Double price;
    private Boolean active;
    private String imageUrl;
    private String imagePublicId;
    private Integer preparationTime;
    private String recipe;
    private List<String> ingredients = new ArrayList<>();

    @CommandHandler
    public MenuItemAggregate(CreateMenuItemCommand command) {
        MenuItemCreatedEvent event = new MenuItemCreatedEvent();
        BeanUtils.copyProperties(command, event);
        apply(event);
    }

    @EventSourcingHandler
    public void on(MenuItemCreatedEvent event) {
        this.menuItemId = event.getMenuItemId();
        this.name = event.getName();
        this.categoryId = event.getCategoryId();
        this.description = event.getDescription();
        this.price = event.getPrice();
        this.active = event.getActive();
        this.imageUrl = event.getImageUrl();
        this.imagePublicId = event.getImagePublicId();
        this.preparationTime = event.getPreparationTime();
        this.recipe = event.getRecipe();
        this.ingredients = event.getIngredients() != null ? new ArrayList<>(event.getIngredients()) : new ArrayList<>();
    }

    @CommandHandler
    public void handle(UpdateMenuItemCommand command) {
        MenuItemUpdatedEvent event = new MenuItemUpdatedEvent();
        BeanUtils.copyProperties(command, event);
        apply(event);
    }

    @EventSourcingHandler
    public void on(MenuItemUpdatedEvent event) {
        this.name = event.getName();
        this.categoryId = event.getCategoryId();
        this.description = event.getDescription();
        this.price = event.getPrice();
        this.imageUrl = event.getImageUrl();
        this.imagePublicId = event.getImagePublicId();
        this.preparationTime = event.getPreparationTime();
        this.recipe = event.getRecipe();
        this.ingredients = event.getIngredients() != null ? new ArrayList<>(event.getIngredients()) : new ArrayList<>();
    }

    @CommandHandler
    public void handle(ToggleMenuItemActiveCommand command) {
        MenuItemActiveToggledEvent event = new MenuItemActiveToggledEvent(command.getMenuItemId(), command.isActive());
        apply(event);
    }

    @EventSourcingHandler
    public void on(MenuItemActiveToggledEvent event) {
        this.active = event.isActive();
    }

    @CommandHandler
    public void handle(AttachIngredientsCommand command) {
        MenuItemIngredientsAttachedEvent event = new MenuItemIngredientsAttachedEvent(command.getMenuItemId(), command.getIngredients());
        apply(event);
    }

    @EventSourcingHandler
    public void on(MenuItemIngredientsAttachedEvent event) {
        this.ingredients = event.getIngredients() != null ? new ArrayList<>(event.getIngredients()) : new ArrayList<>();
    }

    @CommandHandler
    public void handle(DeleteMenuItemCommand command) {
        MenuItemDeletedEvent event = new MenuItemDeletedEvent(command.getMenuItemId());
        apply(event);
    }

    @EventSourcingHandler
    public void on(MenuItemDeletedEvent event) {
        this.active = false;
    }

    @CommandHandler
    public void handle(AutoToggleMenuItemCommand command) {
        boolean shouldBeActive = !"INVENTORY_LOW".equals(command.getReason()) && 
                                !"INVENTORY_OUT".equals(command.getReason());
        
        MenuItemAutoToggledEvent event = new MenuItemAutoToggledEvent(
            command.getMenuItemId(), 
            shouldBeActive, 
            command.getReason(), 
            command.getIngredientId()
        );
        apply(event);
    }

    @EventSourcingHandler
    public void on(MenuItemAutoToggledEvent event) {
        this.active = event.getActive();
    }
}


