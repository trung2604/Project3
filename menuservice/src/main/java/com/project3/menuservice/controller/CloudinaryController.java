package com.project3.menuservice.controller;

import com.project3.menuservice.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cloudinary")
public class CloudinaryController {

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * Get Cloudinary signature for frontend upload
     */
    @GetMapping("/signature")
    public ResponseEntity<Map<String, String>> getSignature() {
        try {
            Map<String, String> signature = cloudinaryService.generateSignature();
            return ResponseEntity.ok(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
