package com.project3.userservice.repository;

import com.project3.userservice.dto.identity.TokenExchangeRequest;
import com.project3.userservice.dto.identity.TokenExchangeResponse;
import com.project3.userservice.dto.identity.UserCreationRequest;
import com.project3.userservice.dto.identity.UserLoginRequest;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "identity-client", url = "${idp.url}")
public interface IdentityClient {
    @PostMapping(
            value = "{path}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    TokenExchangeResponse exchangeClientToken(
            @QueryMap TokenExchangeRequest request, 
            @PathVariable("path") String path);

    @PostMapping(
            value = "{path}/users",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> createUser(
            @RequestBody UserCreationRequest request,
            @RequestHeader("Authorization") String authorization,
            @PathVariable("path") String path);
    
    @PostMapping(
            value = "{path}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    TokenExchangeResponse loginUser(
            @QueryMap UserLoginRequest request,
            @PathVariable("path") String path);
    
    @PutMapping(
            value = "{path}/users/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> updateUser(
            @PathVariable("path") String path,
            @PathVariable("userId") String userId,
            @RequestBody UserCreationRequest request,
            @RequestHeader("Authorization") String authorization);
}
