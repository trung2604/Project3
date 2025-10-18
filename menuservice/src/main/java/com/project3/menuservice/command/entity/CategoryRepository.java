package com.project3.menuservice.command.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, String> {
    
    @Query("SELECT c FROM Category c WHERE c.type = :type AND c.active = true")
    List<Category> findByTypeAndActive(@Param("type") String type);
    
    @Query("SELECT c FROM Category c WHERE c.active = true")
    List<Category> findAllActive();
}
