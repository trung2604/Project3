package com.project3.menuservice.command.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MenuItemIngredientRepository extends JpaRepository<MenuItemIngredient, Long> {
    
    // Use menuItem.menuItemId to access the ID through the relationship
    @Query("SELECT mi FROM MenuItemIngredient mi WHERE mi.menuItem.menuItemId = :menuItemId")
    List<MenuItemIngredient> findByMenuItemId(@Param("menuItemId") String menuItemId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM MenuItemIngredient mi WHERE mi.menuItem.menuItemId = :menuItemId")
    void deleteByMenuItemId(@Param("menuItemId") String menuItemId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM MenuItemIngredient mi WHERE mi.menuItem.menuItemId = :menuItemId AND mi.ingredientId = :ingredientId")
    void deleteByMenuItemIdAndIngredientId(@Param("menuItemId") String menuItemId, @Param("ingredientId") String ingredientId);
}

