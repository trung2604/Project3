package com.project3.menuservice.query.projection;

import com.project3.menuservice.command.entity.MenuItem;
import com.project3.menuservice.command.entity.MenuItemIngredient;
import com.project3.menuservice.command.entity.MenuItemIngredientRepository;
import com.project3.menuservice.command.entity.MenuItemRepository;
import com.project3.menuservice.query.dto.MenuItemResponse;
import com.project3.menuservice.query.dto.PagedMenuItemResponse;
import com.project3.menuservice.query.queries.GetAllMenuItemsQuery;
import com.project3.menuservice.query.queries.GetMenuItemByIdQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class MenuProjection {

    @Autowired
    private MenuItemRepository menuItemRepository;
    
    @Autowired
    private MenuItemIngredientRepository menuItemIngredientRepository;

    @QueryHandler
    @Transactional(readOnly = true)
    public PagedMenuItemResponse getAll(GetAllMenuItemsQuery query) {
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 20;

        Pageable pageable = PageRequest.of(page, size);
        
        Page<MenuItem> menuItemPage = menuItemRepository.findByFilters(
                query.getCategoryId(),
                query.getActive(),
                query.getMinPrice(),
                query.getMaxPrice(),
                query.getSearch(),
                pageable
        );

        List<MenuItemResponse> result = new ArrayList<>();
        for (MenuItem item : menuItemPage.getContent()) {
            List<MenuItemIngredient> ingredients = menuItemIngredientRepository.findByMenuItemId(item.getMenuItemId());
            
            // Set the fetched ingredients to the menu item and ensure bidirectional relationship
            if (item.getMenuItemIngredients() == null) {
                item.setMenuItemIngredients(new ArrayList<>());
            }
            item.getMenuItemIngredients().clear();
            // Set menuItem reference for each ingredient to ensure proper relationship
            if (ingredients != null) {
                for (MenuItemIngredient ingredient : ingredients) {
                    ingredient.setMenuItem(item); // Ensure bidirectional relationship
                }
                item.getMenuItemIngredients().addAll(ingredients);
            }
            
            MenuItemResponse response = MenuItemResponse.fromEntity(item);
            result.add(response);
        }

        return new PagedMenuItemResponse(
                result,
                menuItemPage.getNumber(),
                menuItemPage.getSize(),
                menuItemPage.getTotalElements(),
                menuItemPage.getTotalPages()
        );
    }

    @QueryHandler
    @Transactional(readOnly = true)
    public MenuItemResponse getById(GetMenuItemByIdQuery query) {
        MenuItem item = menuItemRepository.findWithDetailsById(query.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        
        List<MenuItemIngredient> ingredients = menuItemIngredientRepository.findByMenuItemId(query.getMenuItemId());
        if (item.getMenuItemIngredients() == null) {
            item.setMenuItemIngredients(new ArrayList<>());
        }
        item.getMenuItemIngredients().clear();
        if (ingredients != null) {
            for (MenuItemIngredient ingredient : ingredients) {
                ingredient.setMenuItem(item);
            }
            item.getMenuItemIngredients().addAll(ingredients);
        }
        
        return MenuItemResponse.fromEntity(item);
    }
}
