package com.project3.menuservice.query.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.menuservice.query.dto.ComboResponse;
import com.project3.menuservice.query.queries.GetAllCombosQuery;
import com.project3.menuservice.query.queries.GetComboByIdQuery;
import com.project3.menuservice.query.queries.GetCombosByMenuItemQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant/combo")
@Slf4j
public class ComboQueryController {

    @Autowired
    private QueryGateway queryGateway;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ComboResponse>>> getAll() {
        try {
            GetAllCombosQuery query = new GetAllCombosQuery();
            List<ComboResponse> combos = queryGateway.query(query, ResponseTypes.multipleInstancesOf(ComboResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(combos, "Combos retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving combos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve combos: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ComboResponse>> getById(@PathVariable("id") String id) {
        try {
            GetComboByIdQuery query = new GetComboByIdQuery(id);
            ComboResponse combo = queryGateway.query(query, ResponseTypes.instanceOf(ComboResponse.class)).join();
            if (combo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.notFound("Combo not found with id: " + id));
            }
            return ResponseEntity.ok(ApiResponseDTO.success(combo, "Combo retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving combo {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound("Combo not found with id: " + id));
        }
    }

    @GetMapping("/menu-item/{menuItemId}")
    public ResponseEntity<ApiResponseDTO<List<ComboResponse>>> getByMenuItem(@PathVariable("menuItemId") String menuItemId) {
        try {
            GetCombosByMenuItemQuery query = new GetCombosByMenuItemQuery(menuItemId);
            List<ComboResponse> combos = queryGateway.query(query, ResponseTypes.multipleInstancesOf(ComboResponse.class)).join();
            return ResponseEntity.ok(ApiResponseDTO.success(combos, "Combos retrieved successfully by menu item: " + menuItemId));
        } catch (Exception e) {
            log.error("Error retrieving combos by menu item {}: {}", menuItemId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to retrieve combos by menu item: " + e.getMessage(), 500));
        }
    }
}
