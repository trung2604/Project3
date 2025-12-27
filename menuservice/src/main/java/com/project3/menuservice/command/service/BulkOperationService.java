package com.project3.menuservice.command.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for handling bulk operations
 * Encapsulates bulk operation logic for better cohesion
 */
@Service
@Slf4j
public class BulkOperationService {
    
    /**
     * Executes bulk operation and returns result statistics
     * 
     * @param ids List of IDs to process
     * @param operation Function that creates and executes command for each ID
     * @return Map with success, failed, and total counts
     */
    public Map<String, Object> executeBulkOperation(
            List<String> ids, 
            Function<String, Boolean> operation) {
        int success = 0;
        int failed = 0;
        
        for (String id : ids) {
            try {
                if (operation.apply(id)) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.warn("Failed to process id {}: {}", id, e.getMessage());
                failed++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("total", ids.size());
        
        return result;
    }
}

