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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class MenuProjection {

    @Autowired
    private MenuItemRepository menuItemRepository;

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
                pageable
        );

        List<MenuItemResponse> result = new ArrayList<>();
        for (MenuItem item : menuItemPage.getContent()) {
            // Load ingredients separately to avoid serialization issues
            MenuItem fullItem = menuItemRepository.findWithDetailsById(item.getMenuItemId()).orElse(item);
            result.add(MenuItemResponse.fromEntity(fullItem));
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
        return MenuItemResponse.fromEntity(item);
    }
}
