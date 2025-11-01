package com.project3.userservice.controller;

import com.project3.userservice.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<Map<String, String>> getSignature() {
        try {
            Map<String, String> signature = cloudinaryService.generateSignature();
            log.debug("Generated Cloudinary signature successfully");
            return ResponseEntity.ok(signature);
        } catch (Exception e) {
            log.error("Failed to generate Cloudinary signature: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

