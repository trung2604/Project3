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

    @DeleteMapping(
            value = "{path}/users/{userId}"
    )
    ResponseEntity<Void> deleteUser(
            @PathVariable("path") String path,
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authorization);

    // Removed email verify execution methods to use custom userservice flow

    @GetMapping(
            value = "{path}/users/{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    java.util.Map<String, Object> getUser(
            @PathVariable("path") String path,
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authorization);

    @PutMapping(
            value = "{path}/users/{userId}/execute-actions-email",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> executeActionsEmail(
            @PathVariable("path") String path,
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestBody java.util.List<String> actions);
    
    @PostMapping(
            value = "{path}/users/{userId}/execute-actions-email",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> executeActionsEmailPost(
            @PathVariable("path") String path,
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestBody java.util.List<String> actions);
}
