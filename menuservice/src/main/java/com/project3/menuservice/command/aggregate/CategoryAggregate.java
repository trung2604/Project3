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

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Aggregate
public class CategoryAggregate {

    @AggregateIdentifier
    private String categoryId;
    private String name;
    private String description;
    private String type;
    private Boolean active;

    @CommandHandler
    public CategoryAggregate(CreateCategoryCommand command) {
        CategoryCreatedEvent event = new CategoryCreatedEvent();
        BeanUtils.copyProperties(command, event);
        apply(event);
    }

    @EventSourcingHandler
    public void on(CategoryCreatedEvent event) {
        this.categoryId = event.getCategoryId();
        this.name = event.getName();
        this.description = event.getDescription();
        this.type = event.getType();
        this.active = event.getActive();
    }

    @CommandHandler
    public void handle(UpdateCategoryCommand command) {
        CategoryUpdatedEvent event = new CategoryUpdatedEvent();
        BeanUtils.copyProperties(command, event);
        apply(event);
    }

    @EventSourcingHandler
    public void on(CategoryUpdatedEvent event) {
        this.name = event.getName();
        this.description = event.getDescription();
        this.type = event.getType();
    }

    @CommandHandler
    public void handle(ToggleCategoryActiveCommand command) {
        CategoryActiveToggledEvent event = new CategoryActiveToggledEvent(command.getCategoryId(), command.getActive());
        apply(event);
    }

    @EventSourcingHandler
    public void on(CategoryActiveToggledEvent event) {
        this.active = event.getActive();
    }

    @CommandHandler
    public void handle(DeleteCategoryCommand command) {
        CategoryDeletedEvent event = new CategoryDeletedEvent(command.getCategoryId());
        apply(event);
    }

    @EventSourcingHandler
    public void on(CategoryDeletedEvent event) {
        this.active = false;
    }
}
