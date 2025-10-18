package com.project3.menuservice.command.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, String> {
    
    @Query("SELECT c FROM Combo c WHERE c.active = true")
    List<Combo> findAllActive();
    
    @Query("SELECT c FROM Combo c WHERE :menuItemId MEMBER OF c.menuItemIds AND c.active = true")
    List<Combo> findByMenuItemIdAndActive(@Param("menuItemId") String menuItemId);
}
