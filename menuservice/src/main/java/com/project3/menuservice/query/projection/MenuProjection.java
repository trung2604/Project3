package com.project3.menuservice.query.projection;

import com.project3.menuservice.command.entity.MenuItem;
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

import java.util.ArrayList;
import java.util.List;

@Component
public class MenuProjection {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @QueryHandler
    public PagedMenuItemResponse getAll(GetAllMenuItemsQuery query) {
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 20;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MenuItem> menuItemPage = menuItemRepository.findByFilters(
            query.getCategoryId(),
            query.getActive(),
            query.getMinPrice(),
            query.getMaxPrice(),
            pageable
        );
        
        List<MenuItemResponse> result = new ArrayList<>();
        menuItemPage.getContent().forEach(item -> {
            MenuItemResponse dto = new MenuItemResponse();
            dto.setMenuItemId(item.getMenuItemId());
            dto.setName(item.getName());
            dto.setCategoryId(item.getCategory() != null ? item.getCategory().getCategoryId() : null);
            dto.setCategoryName(item.getCategory() != null ? item.getCategory().getName() : null);
            dto.setDescription(item.getDescription());
            dto.setPrice(item.getPrice());
            dto.setActive(item.getActive());
            dto.setIngredients(item.getIngredients());
            result.add(dto);
        });
        
        return new PagedMenuItemResponse(
            result,
            menuItemPage.getNumber(),
            menuItemPage.getSize(),
            menuItemPage.getTotalElements(),
            menuItemPage.getTotalPages()
        );
    }

    @QueryHandler
    public MenuItemResponse getById(GetMenuItemByIdQuery query) {
        MenuItem item = menuItemRepository.findById(query.getMenuItemId()).orElseThrow(() -> new RuntimeException("Menu item not found"));
        MenuItemResponse dto = new MenuItemResponse();
        dto.setMenuItemId(item.getMenuItemId());
        dto.setName(item.getName());
        dto.setCategoryId(item.getCategory() != null ? item.getCategory().getCategoryId() : null);
        dto.setCategoryName(item.getCategory() != null ? item.getCategory().getName() : null);
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setActive(item.getActive());
        dto.setIngredients(item.getIngredients());
        return dto;
    }
}


