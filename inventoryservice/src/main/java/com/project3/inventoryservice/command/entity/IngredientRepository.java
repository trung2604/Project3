package com.project3.inventoryservice.command.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, String> {
    
    List<Ingredient> findByActiveTrue();
    
    List<Ingredient> findByCategory(String category);
    
    @Query("SELECT i FROM Ingredient i WHERE i.currentStock <= i.minStockLevel AND i.active = true")
    List<Ingredient> findLowStockIngredients();
    
    @Query("SELECT i FROM Ingredient i WHERE i.expiryDate <= :expiryDate AND i.active = true")
    List<Ingredient> findExpiringIngredients(@Param("expiryDate") LocalDate expiryDate);
    
    @Query(value = "SELECT * FROM ingredients i WHERE " +
           "(:category IS NULL OR i.category = :category) AND " +
           "(:active IS NULL OR i.active = :active) AND " +
           "(:minStock IS NULL OR i.current_stock >= :minStock) AND " +
           "(:maxStock IS NULL OR i.current_stock <= :maxStock) AND " +
           "(:search IS NULL OR (i.name IS NOT NULL AND LOWER(CAST(i.name AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%')) OR " +
           "(i.description IS NOT NULL AND LOWER(CAST(i.description AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%')) OR " +
           "(i.supplier_name IS NOT NULL AND LOWER(CAST(i.supplier_name AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%'))) " +
           "ORDER BY i.ingredient_id DESC",
           countQuery = "SELECT COUNT(*) FROM ingredients i WHERE " +
           "(:category IS NULL OR i.category = :category) AND " +
           "(:active IS NULL OR i.active = :active) AND " +
           "(:minStock IS NULL OR i.current_stock >= :minStock) AND " +
           "(:maxStock IS NULL OR i.current_stock <= :maxStock) AND " +
           "(:search IS NULL OR (i.name IS NOT NULL AND LOWER(CAST(i.name AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%')) OR " +
           "(i.description IS NOT NULL AND LOWER(CAST(i.description AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%')) OR " +
           "(i.supplier_name IS NOT NULL AND LOWER(CAST(i.supplier_name AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%')))",
           nativeQuery = true)
    Page<Ingredient> findByFilters(@Param("category") String category,
                                  @Param("active") Boolean active,
                                  @Param("minStock") Double minStock,
                                  @Param("maxStock") Double maxStock,
                                  @Param("search") String search,
                                  Pageable pageable);
    
    Optional<Ingredient> findByNameIgnoreCase(String name);
}
