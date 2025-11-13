package com.project3.orderservice.command.entity;

import com.project3.orderservice.command.enums.OrderStatus;
import com.project3.orderservice.command.enums.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRespository extends JpaRepository<Order, String> {
    List<Order> findByOrderStatus(OrderStatus status);
    List<Order> findByOrderType(OrderType type);
    List<Order> findByCustomerId(String customerId);
    
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status AND o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findByStatusAndDateRange(@Param("status") OrderStatus status,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}
