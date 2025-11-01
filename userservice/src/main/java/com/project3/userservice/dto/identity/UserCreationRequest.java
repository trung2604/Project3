package com.project3.userservice.dto.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreationRequest {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    
    @JsonProperty("enabled")
    private Boolean enabled = true;
    
    @JsonProperty("emailVerified")
    private Boolean emailVerified = false;
    
    private List<Credentials> credentials;
    
    @JsonProperty("attributes")
    private Map<String, List<String>> attributes;
    
    public void setPassword(String password) {
        if (password != null && !password.isEmpty()) {
            if (credentials == null) {
                credentials = new ArrayList<>();
            }
            Credentials cred = Credentials.builder()
                    .type("password")
                    .value(password)
                    .temporary(false)
                    .build();
            credentials.add(cred);
        }
    }
    
    public void addAttribute(String key, String value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        if (value != null && !value.isEmpty()) {
            attributes.put(key, List.of(value));
        }
    }
    
    public void setPhone(String phone) {
        addAttribute("phone", phone);
    }
    
    public void setAddress(String address) {
        addAttribute("address", address);
    }
    
    public void setRole(String role) {
        addAttribute("role", role);
    }
    
    public void setAvatarUrl(String avatarUrl) {
        addAttribute("avatarUrl", avatarUrl);
    }
    
    public void setAvatarPublicId(String avatarPublicId) {
        addAttribute("avatarPublicId", avatarPublicId);
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth != null) {
            addAttribute("dateOfBirth", dateOfBirth.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
    }
}
