package com.project3.menuservice.command.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, String> {
    
    @Query("SELECT m FROM MenuItem m LEFT JOIN FETCH m.category WHERE " +
           "(:categoryId IS NULL OR m.category.categoryId = :categoryId) AND " +
           "(:active IS NULL OR m.active = :active) AND " +
           "(:minPrice IS NULL OR m.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR m.price <= :maxPrice)")
    Page<MenuItem> findByFilters(@Param("categoryId") String categoryId,
                                 @Param("active") Boolean active,
                                 @Param("minPrice") Double minPrice,
                                 @Param("maxPrice") Double maxPrice,
                                 Pageable pageable);

    @EntityGraph(attributePaths = {"category", "ingredients"})
    @Query("SELECT m FROM MenuItem m WHERE m.menuItemId = :menuItemId")
    Optional<MenuItem> findWithDetailsById(@Param("menuItemId") String menuItemId);
}


