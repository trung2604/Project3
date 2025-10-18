package com.project3.inventoryservice.query.projection;

import com.project3.inventoryservice.command.entity.Ingredient;
import com.project3.inventoryservice.command.entity.IngredientRepository;
import com.project3.inventoryservice.query.dto.IngredientResponse;
import com.project3.inventoryservice.query.dto.PagedIngredientResponse;
import com.project3.inventoryservice.query.queries.GetAllIngredientsQuery;
import com.project3.inventoryservice.query.queries.GetIngredientByIdQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class IngredientProjection {
    
    @Autowired
    private IngredientRepository ingredientRepository;
    
    @QueryHandler
    public PagedIngredientResponse getAll(GetAllIngredientsQuery query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
        Page<Ingredient> ingredientPage = ingredientRepository.findByFilters(
            query.getCategory(),
            query.getActive(),
            query.getMinStock(),
            query.getMaxStock(),
            pageable
        );
        
        List<IngredientResponse> ingredients = ingredientPage.getContent().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        
        return new PagedIngredientResponse(
            ingredients,
            ingredientPage.getNumber(),
            ingredientPage.getTotalPages(),
            ingredientPage.getTotalElements(),
            ingredientPage.getSize(),
            ingredientPage.hasNext(),
            ingredientPage.hasPrevious()
        );
    }
    
    @QueryHandler
    public IngredientResponse getById(GetIngredientByIdQuery query) {
        Ingredient ingredient = ingredientRepository.findById(query.getIngredientId()).orElse(null);
        return ingredient != null ? mapToResponse(ingredient) : null;
    }
    
    private IngredientResponse mapToResponse(Ingredient ingredient) {
        IngredientResponse response = new IngredientResponse();
        response.setIngredientId(ingredient.getIngredientId());
        response.setName(ingredient.getName());
        response.setDescription(ingredient.getDescription());
        response.setUnit(ingredient.getUnit());
        response.setCurrentStock(ingredient.getCurrentStock());
        response.setMinStockLevel(ingredient.getMinStockLevel());
        response.setMaxStockLevel(ingredient.getMaxStockLevel());
        response.setExpiryDate(ingredient.getExpiryDate());
        response.setActive(ingredient.getActive());
        response.setCreatedAt(ingredient.getCreatedAt());
        response.setUpdatedAt(ingredient.getUpdatedAt());
        response.setSupplierName(ingredient.getSupplierName());
        response.setSupplierContact(ingredient.getSupplierContact());
        response.setUnitCost(ingredient.getUnitCost());
        response.setCurrency(ingredient.getCurrency());
        response.setCategory(ingredient.getCategory());
        return response;
    }
}
