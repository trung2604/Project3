package com.project3.menuservice.command.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, String> {
    
    @Query(value = "SELECT mi.* FROM menu_items mi LEFT JOIN categories c ON c.category_id = mi.category_id WHERE " +
           "(:categoryId IS NULL OR c.category_id = :categoryId) AND " +
           "(:active IS NULL OR mi.active = :active) AND " +
           "(:minPrice IS NULL OR mi.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR mi.price <= :maxPrice) AND " +
           "(:search IS NULL OR (mi.name IS NOT NULL AND LOWER(CAST(mi.name AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%')) OR " +
           "(mi.description IS NOT NULL AND LOWER(CAST(mi.description AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%'))) " +
           "ORDER BY mi.menu_item_id DESC",
           countQuery = "SELECT COUNT(*) FROM menu_items mi LEFT JOIN categories c ON c.category_id = mi.category_id WHERE " +
           "(:categoryId IS NULL OR c.category_id = :categoryId) AND " +
           "(:active IS NULL OR mi.active = :active) AND " +
           "(:minPrice IS NULL OR mi.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR mi.price <= :maxPrice) AND " +
           "(:search IS NULL OR (mi.name IS NOT NULL AND LOWER(CAST(mi.name AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%')) OR " +
           "(mi.description IS NOT NULL AND LOWER(CAST(mi.description AS text)) LIKE LOWER('%' || CAST(:search AS text) || '%')))",
           nativeQuery = true)
    Page<MenuItem> findByFilters(@Param("categoryId") String categoryId,
                                 @Param("active") Boolean active,
                                 @Param("minPrice") Double minPrice,
                                 @Param("maxPrice") Double maxPrice,
                                 @Param("search") String search,
                                 Pageable pageable);

    @EntityGraph(attributePaths = {"category", "ingredients"})
    @Query("SELECT m FROM MenuItem m WHERE m.menuItemId = :menuItemId")
    Optional<MenuItem> findWithDetailsById(@Param("menuItemId") String menuItemId);
}


