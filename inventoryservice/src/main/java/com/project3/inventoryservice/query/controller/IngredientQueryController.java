package com.project3.inventoryservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.inventoryservice.query.dto.IngredientResponse;
import com.project3.inventoryservice.query.dto.PagedIngredientResponse;
import com.project3.inventoryservice.query.queries.GetAllIngredientsQuery;
import com.project3.inventoryservice.query.queries.GetIngredientByIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/ingredient")
@Tag(name = "Ingredient Queries", description = "APIs để truy vấn thông tin nguyên liệu")
@Slf4j
public class IngredientQueryController {
    
    @Autowired
    private QueryGateway queryGateway;
    
    @GetMapping
    @Operation(summary = "Lấy danh sách nguyên liệu", description = "Lấy danh sách nguyên liệu với các bộ lọc, tìm kiếm, sắp xếp và phân trang")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    })
    public ResponseEntity<ApiResponseDTO<PagedIngredientResponse>> getAll(
            @Parameter(description = "Loại nguyên liệu") @RequestParam(required = false) String category,
            @Parameter(description = "Trạng thái hoạt động") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Tồn kho tối thiểu") @RequestParam(required = false) Double minStock,
            @Parameter(description = "Tồn kho tối đa") @RequestParam(required = false) Double maxStock,
            @Parameter(description = "Tìm kiếm theo tên, mô tả hoặc nhà cung cấp") @RequestParam(required = false) String search,
            @Parameter(description = "Sắp xếp theo: name, currentStock, unitCost, expiryDate, createdAt") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Hướng sắp xếp: asc, desc") @RequestParam(required = false) String sortDirection,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "10") int size) {
        
        try {
            GetAllIngredientsQuery query = new GetAllIngredientsQuery(
                category, active, minStock, maxStock, search, sortBy, sortDirection, page, size
            );
            
            PagedIngredientResponse response = queryGateway.query(query, ResponseTypes.instanceOf(PagedIngredientResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Ingredients retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving ingredients: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve ingredients: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/{ingredientId}")
    @Operation(summary = "Lấy nguyên liệu theo ID", description = "Lấy thông tin chi tiết của một nguyên liệu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nguyên liệu")
    })
    public ResponseEntity<ApiResponseDTO<IngredientResponse>> getById(
            @Parameter(description = "ID của nguyên liệu") @PathVariable String ingredientId) {
        try {
            GetIngredientByIdQuery query = new GetIngredientByIdQuery(ingredientId);
            IngredientResponse ingredient = queryGateway.query(query, ResponseTypes.instanceOf(IngredientResponse.class)).join();
            if (ingredient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.notFound("Ingredient not found with id: " + ingredientId));
            }
            return ResponseEntity.ok(ApiResponseDTO.success(ingredient, "Ingredient retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving ingredient {}: {}", ingredientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound("Ingredient not found with id: " + ingredientId));
        }
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Lấy nguyên liệu tồn kho thấp", description = "Lấy danh sách nguyên liệu có tồn kho thấp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    public ResponseEntity<ApiResponseDTO<PagedIngredientResponse>> getLowStockIngredients(
            @Parameter(description = "Tìm kiếm") @RequestParam(required = false) String search,
            @Parameter(description = "Sắp xếp theo") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Hướng sắp xếp") @RequestParam(required = false) String sortDirection,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "10") int size) {
        
        try {
            GetAllIngredientsQuery query = new GetAllIngredientsQuery(
                null, true, null, null, search, sortBy, sortDirection, page, size
            );
            
            PagedIngredientResponse response = queryGateway.query(query, ResponseTypes.instanceOf(PagedIngredientResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Low stock ingredients retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving low stock ingredients: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve low stock ingredients: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Lấy nguyên liệu theo loại", description = "Lấy danh sách nguyên liệu theo loại")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    public ResponseEntity<ApiResponseDTO<PagedIngredientResponse>> getByCategory(
            @Parameter(description = "Loại nguyên liệu") @PathVariable String category,
            @Parameter(description = "Tìm kiếm") @RequestParam(required = false) String search,
            @Parameter(description = "Sắp xếp theo") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Hướng sắp xếp") @RequestParam(required = false) String sortDirection,
            @Parameter(description = "Số trang") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "10") int size) {
        
        try {
            GetAllIngredientsQuery query = new GetAllIngredientsQuery(
                category, true, null, null, search, sortBy, sortDirection, page, size
            );
            
            PagedIngredientResponse response = queryGateway.query(query, ResponseTypes.instanceOf(PagedIngredientResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Ingredients retrieved successfully by category: " + category));
        } catch (Exception e) {
            log.error("Error retrieving ingredients by category {}: {}", category, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve ingredients by category: " + e.getMessage(), 500));
        }
    }
}
