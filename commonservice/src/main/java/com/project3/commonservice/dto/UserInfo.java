package com.project3.commonservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private String userId;
    private String username;
    private String email;
    private String role;
    private String status;
    
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}

