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
public class ComboAggregate {

    @AggregateIdentifier
    private String comboId;
    private String name;
    private String description;
    private Double price;
    private Double discount;
    private List<String> menuItemIds = new ArrayList<>();
    private Boolean active;

    @CommandHandler
    public ComboAggregate(CreateComboCommand command) {
        ComboCreatedEvent event = new ComboCreatedEvent();
        BeanUtils.copyProperties(command, event);
        apply(event);
    }

    @EventSourcingHandler
    public void on(ComboCreatedEvent event) {
        this.comboId = event.getComboId();
        this.name = event.getName();
        this.description = event.getDescription();
        this.price = event.getPrice();
        this.discount = event.getDiscount();
        this.menuItemIds = event.getMenuItemIds() != null ? new ArrayList<>(event.getMenuItemIds()) : new ArrayList<>();
        this.active = event.getActive();
    }

    @CommandHandler
    public void handle(UpdateComboCommand command) {
        ComboUpdatedEvent event = new ComboUpdatedEvent();
        BeanUtils.copyProperties(command, event);
        apply(event);
    }

    @EventSourcingHandler
    public void on(ComboUpdatedEvent event) {
        this.name = event.getName();
        this.description = event.getDescription();
        this.price = event.getPrice();
        this.discount = event.getDiscount();
        this.menuItemIds = event.getMenuItemIds() != null ? new ArrayList<>(event.getMenuItemIds()) : new ArrayList<>();
    }

    @CommandHandler
    public void handle(ToggleComboActiveCommand command) {
        ComboActiveToggledEvent event = new ComboActiveToggledEvent(command.getComboId(), command.getActive());
        apply(event);
    }

    @EventSourcingHandler
    public void on(ComboActiveToggledEvent event) {
        this.active = event.getActive();
    }

    @CommandHandler
    public void handle(AddMenuItemToComboCommand command) {
        if (!this.menuItemIds.contains(command.getMenuItemId())) {
            MenuItemAddedToComboEvent event = new MenuItemAddedToComboEvent(command.getComboId(), command.getMenuItemId());
            apply(event);
        }
    }

    @EventSourcingHandler
    public void on(MenuItemAddedToComboEvent event) {
        if (!this.menuItemIds.contains(event.getMenuItemId())) {
            this.menuItemIds.add(event.getMenuItemId());
        }
    }

    @CommandHandler
    public void handle(RemoveMenuItemFromComboCommand command) {
        if (this.menuItemIds.contains(command.getMenuItemId())) {
            MenuItemRemovedFromComboEvent event = new MenuItemRemovedFromComboEvent(command.getComboId(), command.getMenuItemId());
            apply(event);
        }
    }

    @EventSourcingHandler
    public void on(MenuItemRemovedFromComboEvent event) {
        this.menuItemIds.remove(event.getMenuItemId());
    }

    @CommandHandler
    public void handle(DeleteComboCommand command) {
        ComboDeletedEvent event = new ComboDeletedEvent(command.getComboId());
        apply(event);
    }

    @EventSourcingHandler
    public void on(ComboDeletedEvent event) {
        this.active = false;
    }
}
