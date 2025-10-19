package com.project3.orderservice.command.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRespository extends JpaRepository<Order, String> {
}
