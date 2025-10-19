package com.project3.orderservice.command.aggregate;

import com.project3.orderservice.command.commands.CreateOrderCommand;
import com.project3.orderservice.command.event.CreateOrderEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Aggregate
public class CreateOrderAggregate {
    @AggregateIdentifier
    private String orderId;
    private String customerId;
    private String orderStatus;
    private Double orderAmount;
    private String orderDate;
    private Long vat;

    @CommandHandler
    public CreateOrderAggregate(CreateOrderCommand command) {
        CreateOrderEvent event = new CreateOrderEvent();
        BeanUtils.copyProperties(command, event);
    }

    @EventSourcingHandler
    public void on(CreateOrderEvent event) {
        this.orderId = event.getOrderID();
        this.customerId = event.getCustomerId();
        this.orderStatus = event.getOrderStatus();
        this.orderAmount = event.getOrderAmount();
        this.orderDate = event.getOrderDate();
        this.vat = event.getVat();
    }
}
