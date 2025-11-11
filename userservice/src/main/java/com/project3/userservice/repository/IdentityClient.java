package com.project3.userservice.repository;

import com.project3.userservice.dto.identity.TokenExchangeResponse;
import com.project3.userservice.dto.identity.UserCreationRequest;
import com.project3.userservice.dto.identity.PasswordResetRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "identity-client", url = "${idp.url}")
public interface IdentityClient {
    @PostMapping(
            value = "{path}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @feign.Headers("Content-Type: application/x-www-form-urlencoded")
    TokenExchangeResponse exchangeClientToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "scope", required = false) String scope,
            @PathVariable("path") String path);

    // Form body variant to avoid missing parameters issues
    @PostMapping(
            value = "{path}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @feign.Headers("Content-Type: application/x-www-form-urlencoded")
    TokenExchangeResponse exchangeTokenForm(
            @PathVariable("path") String path,
            @RequestBody MultiValueMap<String, String> form);

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
    @feign.Headers("Content-Type: application/x-www-form-urlencoded")
    TokenExchangeResponse loginUser(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "scope", required = false) String scope,
            @PathVariable("path") String path);

    // Form body variant for password flow
    @PostMapping(
            value = "{path}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    @feign.Headers("Content-Type: application/x-www-form-urlencoded")
    TokenExchangeResponse loginUserForm(
            @PathVariable("path") String path,
            @RequestBody MultiValueMap<String, String> form);
    
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

    @PutMapping(
            value = "{path}/users/{userId}/reset-password",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> resetPassword(
            @PathVariable("path") String path,
            @PathVariable("userId") String userId,
            @RequestBody PasswordResetRequest request,
            @RequestHeader("Authorization") String authorization);
    
    @PutMapping(
            value = "{path}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> updateRealm(
            @PathVariable("path") String path,
            @RequestBody java.util.Map<String, Object> realmConfig,
            @RequestHeader("Authorization") String authorization);
    
    @GetMapping(
            value = "{path}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    java.util.Map<String, Object> getRealm(
            @PathVariable("path") String path,
            @RequestHeader("Authorization") String authorization);
    
    // Role management endpoints
    @GetMapping(
            value = "{path}/roles",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    java.util.List<java.util.Map<String, Object>> getRealmRoles(
            @PathVariable("path") String path,
            @RequestHeader("Authorization") String authorization);
    
    @PostMapping(
            value = "{path}/users/{userId}/role-mappings/realm",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> assignRealmRole(
            @PathVariable("path") String path,
            @PathVariable("userId") String userId,
            @RequestBody java.util.List<java.util.Map<String, Object>> roles,
            @RequestHeader("Authorization") String authorization);
    
    @GetMapping(
            value = "{path}/clients",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    java.util.List<java.util.Map<String, Object>> getClients(
            @PathVariable("path") String path,
            @RequestParam("clientId") String clientId,
            @RequestHeader("Authorization") String authorization);
    
    @GetMapping(
            value = "{path}/clients/{clientUuid}/roles",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    java.util.List<java.util.Map<String, Object>> getClientRoles(
            @PathVariable("path") String path,
            @PathVariable("clientUuid") String clientUuid,
            @RequestHeader("Authorization") String authorization);
    
    @PostMapping(
            value = "{path}/users/{userId}/role-mappings/clients/{clientUuid}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> assignClientRole(
            @PathVariable("path") String path,
            @PathVariable("userId") String userId,
            @PathVariable("clientUuid") String clientUuid,
            @RequestBody java.util.List<java.util.Map<String, Object>> roles,
            @RequestHeader("Authorization") String authorization);
    
    // Create role endpoints
    @PostMapping(
            value = "{path}/roles",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> createRealmRole(
            @PathVariable("path") String path,
            @RequestBody java.util.Map<String, Object> role,
            @RequestHeader("Authorization") String authorization);
    
    @PostMapping(
            value = "{path}/clients/{clientUuid}/roles",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Void> createClientRole(
            @PathVariable("path") String path,
            @PathVariable("clientUuid") String clientUuid,
            @RequestBody java.util.Map<String, Object> role,
            @RequestHeader("Authorization") String authorization);
}
