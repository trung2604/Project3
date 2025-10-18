package com.project3.inventoryservice.query.controller;

import com.project3.inventoryservice.query.dto.IngredientResponse;
import com.project3.inventoryservice.query.dto.PagedIngredientResponse;
import com.project3.inventoryservice.query.queries.GetAllIngredientsQuery;
import com.project3.inventoryservice.query.queries.GetIngredientByIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/ingredient")
@Tag(name = "Ingredient Queries", description = "APIs để truy vấn thông tin nguyên liệu")
public class IngredientQueryController {
    
    @Autowired
    private QueryGateway queryGateway;
    
    @GetMapping
    @Operation(summary = "Lấy danh sách nguyên liệu", description = "Lấy danh sách nguyên liệu với các bộ lọc và phân trang")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    })
    public PagedIngredientResponse getAll(
            @Parameter(description = "Loại nguyên liệu") @RequestParam(required = false) String category,
            @Parameter(description = "Trạng thái hoạt động") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Tồn kho tối thiểu") @RequestParam(required = false) Double minStock,
            @Parameter(description = "Tồn kho tối đa") @RequestParam(required = false) Double maxStock,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "10") int size) {
        
        GetAllIngredientsQuery query = new GetAllIngredientsQuery(
            category, active, minStock, maxStock, page, size
        );
        
        return queryGateway.query(query, ResponseTypes.instanceOf(PagedIngredientResponse.class)).join();
    }
    
    @GetMapping("/{ingredientId}")
    @Operation(summary = "Lấy nguyên liệu theo ID", description = "Lấy thông tin chi tiết của một nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public IngredientResponse getById(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId) {
        GetIngredientByIdQuery query = new GetIngredientByIdQuery(ingredientId);
        return queryGateway.query(query, ResponseTypes.instanceOf(IngredientResponse.class)).join();
    }
    
    @GetMapping("/low-stock")
    @Operation(summary = "Lấy nguyên liệu tồn kho thấp", description = "Lấy danh sách nguyên liệu có tồn kho thấp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    public PagedIngredientResponse getLowStockIngredients(
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "10") int size) {
        
        GetAllIngredientsQuery query = new GetAllIngredientsQuery(
            null, true, null, null, page, size
        );
        
        return queryGateway.query(query, ResponseTypes.instanceOf(PagedIngredientResponse.class)).join();
    }
    
    @GetMapping("/category/{category}")
    @Operation(summary = "Lấy nguyên liệu theo loại", description = "Lấy danh sách nguyên liệu theo loại")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    public PagedIngredientResponse getByCategory(
            @Parameter(description = "Loại nguyên liệu") @PathVariable String category,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "10") int size) {
        
        GetAllIngredientsQuery query = new GetAllIngredientsQuery(
            category, true, null, null, page, size
        );
        
        return queryGateway.query(query, ResponseTypes.instanceOf(PagedIngredientResponse.class)).join();
    }
}
