package com.project3.paymentservice.query.projection;

import com.project3.paymentservice.command.entity.Payment;
import com.project3.paymentservice.command.entity.PaymentRepository;
import com.project3.paymentservice.query.queries.GetPaymentByIdQuery;
import com.project3.paymentservice.query.queries.GetPaymentsByOrderIdQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PaymentProjection {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @QueryHandler
    public Payment handle(GetPaymentByIdQuery query) {
        log.info("Handling GetPaymentByIdQuery for paymentId: {}", query.getPaymentId());
        return paymentRepository.findById(query.getPaymentId())
            .orElse(null);
    }
    
    @QueryHandler
    public List<Payment> handle(GetPaymentsByOrderIdQuery query) {
        log.info("Handling GetPaymentsByOrderIdQuery for orderId: {}", query.getOrderId());
        return paymentRepository.findByOrderId(query.getOrderId());
    }
}
