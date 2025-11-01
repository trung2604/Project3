package com.project3.userservice.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenExchangeRequest {
    private String grant_type;
    private String client_id;
    private String client_secret;
    private String scope;
}
