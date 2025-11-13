package com.project3.orderservice.command.commands;

import com.project3.orderservice.command.dto.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SplitBillCommand {
    @TargetAggregateIdentifier
    private String originalOrderId;
    
    private List<String> newOrderIds;
    private List<List<OrderItemDTO>> splitItems;
    private String splitBy;
}

