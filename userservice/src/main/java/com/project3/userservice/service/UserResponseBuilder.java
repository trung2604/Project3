package com.project3.userservice.service;

import com.project3.userservice.dto.LoginResponseDTO;
import com.project3.userservice.dto.identity.TokenExchangeResponse;
import org.springframework.stereotype.Component;

/**
 * Builder for creating user response DTOs
 * Encapsulates response building logic for better cohesion
 */
@Component
public class UserResponseBuilder {
    
    /**
     * Builds LoginResponseDTO from TokenExchangeResponse
     */
    public LoginResponseDTO buildLoginResponse(TokenExchangeResponse token) {
        LoginResponseDTO response = new LoginResponseDTO();
        response.setAccessToken(token.getAccessToken());
        response.setRefreshToken(token.getRefreshToken() != null ? token.getRefreshToken() : token.getIdToken());
        response.setTokenType(token.getTokenType());
        response.setExpiresIn(Long.parseLong(token.getExpiresIn()));
        response.setRefreshExpiresIn(token.getRefreshExpiresIn() != null ? 
            Long.parseLong(token.getRefreshExpiresIn()) : 1800L);
        return response;
    }
}

