package com.project3.menuservice.query.projection;

import com.project3.menuservice.command.entity.Category;
import com.project3.menuservice.command.entity.CategoryRepository;
import com.project3.menuservice.query.dto.CategoryResponse;
import com.project3.menuservice.query.queries.GetAllCategoriesQuery;
import com.project3.menuservice.query.queries.GetCategoryByIdQuery;
import com.project3.menuservice.query.queries.GetCategoriesByTypeQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CategoryProjection {

    @Autowired
    private CategoryRepository categoryRepository;

    @QueryHandler
    public List<CategoryResponse> getAll(GetAllCategoriesQuery query) {
        List<Category> categories = categoryRepository.findAll();
        List<CategoryResponse> result = new ArrayList<>();
        categories.forEach(category -> {
            CategoryResponse dto = new CategoryResponse();
            BeanUtils.copyProperties(category, dto);
            result.add(dto);
        });
        return result;
    }

    @QueryHandler
    public CategoryResponse getById(GetCategoryByIdQuery query) {
        Category category = categoryRepository.findById(query.getCategoryId()).orElseThrow(() -> new RuntimeException("Category not found"));
        CategoryResponse dto = new CategoryResponse();
        BeanUtils.copyProperties(category, dto);
        return dto;
    }

    @QueryHandler
    public List<CategoryResponse> getByType(GetCategoriesByTypeQuery query) {
        List<Category> categories = categoryRepository.findByTypeAndActive(query.getType());
        List<CategoryResponse> result = new ArrayList<>();
        categories.forEach(category -> {
            CategoryResponse dto = new CategoryResponse();
            BeanUtils.copyProperties(category, dto);
            result.add(dto);
        });
        return result;
    }
}
