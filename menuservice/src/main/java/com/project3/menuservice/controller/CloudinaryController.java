package com.project3.menuservice.controller;

import com.project3.commonservice.dto.ApiResponseDTO;
import com.project3.menuservice.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cloudinary")
@Slf4j
public class CloudinaryController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/signature")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> getSignature(
            @RequestParam(required = false, defaultValue = "restaurant-menu") String folder) {
        try {
            log.info("Generating Cloudinary signature for folder: {}", folder);
            Map<String, String> signature = cloudinaryService.generateSignature(folder);
            log.debug("Generated signature: {}", signature);
            return ResponseEntity.ok(ApiResponseDTO.success(signature, "Cloudinary signature generated successfully"));
        } catch (Exception e) {
            log.error("Failed to generate Cloudinary signature: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Failed to generate Cloudinary signature: " + e.getMessage(), 500));
        }
    }
}
