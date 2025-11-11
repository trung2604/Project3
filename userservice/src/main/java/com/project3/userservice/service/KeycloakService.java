package com.project3.userservice.service;

import com.project3.userservice.config.KeycloakProperties;
import com.project3.userservice.constant.KeycloakConstants;
import com.project3.userservice.dto.identity.*;
import com.project3.userservice.exception.KeycloakException;
import com.project3.userservice.repository.IdentityClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakService {
    
    private final IdentityClient identityClient;
    private final KeycloakProperties keycloakProperties;
    
    private volatile boolean smtpConfigured = false;
    
    public static class KeycloakAdminInfo {
        public TokenExchangeResponse token;
        public String adminPath;
    }
    
    /**
     * Get Keycloak admin info with client credentials
     */
    public KeycloakAdminInfo getKeycloakAdminInfo() {
        String[] realmPaths = getRealmPaths();
        KeycloakAdminInfo info = new KeycloakAdminInfo();
        
        for (String realmPath : realmPaths) {
            try {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("grant_type", KeycloakConstants.GRANT_TYPE_CLIENT_CREDENTIALS);
                form.add("client_id", keycloakProperties.getClientId());
                if (keycloakProperties.getClientSecret() != null) {
                    form.add("client_secret", keycloakProperties.getClientSecret());
                }
                form.add("scope", KeycloakConstants.SCOPE_OPENID);
                info.token = identityClient.exchangeTokenForm(realmPath, form);
                String baseRealmPath = realmPath.startsWith("/auth/") ? "/auth" : "";
                info.adminPath = baseRealmPath + KeycloakConstants.ADMIN_REALMS_PATH + keycloakProperties.getRealm();
                return info;
            } catch (Exception e) {
                log.warn("Failed to get admin token with path {}: {}", realmPath, e.getMessage());
            }
        }
        throw new KeycloakException("Failed to authenticate with Keycloak. Please verify configuration.");
    }
    
    /**
     * Authenticate user with Keycloak using username/password
     */
    public TokenExchangeResponse authenticateUser(String username, String password) {
        String[] realmPaths = getRealmPaths();
        Exception lastException = null;
        
        for (String realmPath : realmPaths) {
            try {
                log.debug("Attempting login with Keycloak for username: {} at {}{}", 
                        username, keycloakProperties.getUrl(), realmPath);
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("grant_type", KeycloakConstants.GRANT_TYPE_PASSWORD);
                form.add("client_id", keycloakProperties.getClientId());
                form.add("client_secret", keycloakProperties.getClientSecret());
                form.add("username", username);
                form.add("password", password);
                form.add("scope", String.join(" ", KeycloakConstants.SCOPE_OPENID,
                        KeycloakConstants.SCOPE_PROFILE, KeycloakConstants.SCOPE_EMAIL));
                TokenExchangeResponse tokenResponse = identityClient.loginUserForm(realmPath, form);
                log.info("Login successful for username: {}", username);
                return tokenResponse;
            } catch (Exception e) {
                String errorDetails = e.getClass().getSimpleName() + ": " + e.getMessage();
                if (e instanceof FeignException) {
                    FeignException feignEx = (FeignException) e;
                    errorDetails = "Keycloak returned " + feignEx.status() + " - " + feignEx.contentUTF8();
                }
                log.warn("Failed to login with path {}: {}", realmPath, errorDetails);
                lastException = e;
            }
        }
        
        log.error("Login failed for username: {}. Last error: {}", 
                username, lastException != null ? lastException.getMessage() : "Unknown");
        return null;
    }
    
    /**
     * Exchange authorization code for tokens
     */
    public TokenExchangeResponse exchangeAuthorizationCode(String code, String redirectUri) {
        log.info("Exchanging authorization code. Redirect URI: {}", redirectUri);
        String realmPath = KeycloakConstants.REALMS_PATH + keycloakProperties.getRealm();
        
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", KeycloakConstants.GRANT_TYPE_AUTHORIZATION_CODE);
            form.add("client_id", keycloakProperties.getClientId());
            if (keycloakProperties.getClientSecret() != null) {
                form.add("client_secret", keycloakProperties.getClientSecret());
            }
            form.add("code", code);
            form.add("redirect_uri", redirectUri);
            form.add("scope", String.join(" ", KeycloakConstants.SCOPE_OPENID,
                    KeycloakConstants.SCOPE_PROFILE, KeycloakConstants.SCOPE_EMAIL));
            TokenExchangeResponse token = identityClient.exchangeTokenForm(realmPath, form);
            log.info("Successfully exchanged authorization code for tokens");
            return token;
        } catch (Exception e) {
            String errorDetails = e.getMessage();
            if (e instanceof FeignException) {
                FeignException feignEx = (FeignException) e;
                errorDetails = String.format("[%d %s] %s", 
                    feignEx.status(), 
                    feignEx.status() == 400 ? "Bad Request" : "Error",
                    feignEx.contentUTF8());
                log.error("Keycloak token exchange error: {}", errorDetails);
            } else {
                log.error("Token exchange error: {}", errorDetails, e);
            }
            
            if (errorDetails != null && errorDetails.contains("invalid_grant")) {
                throw new KeycloakException("Authorization code is invalid or has expired. Please try logging in again.");
            }
            throw new KeycloakException("Failed to exchange authorization code: " + errorDetails, e);
        }
    }
    
    /**
     * Create user in Keycloak
     */
    public String createUser(UserCreationRequest request) {
        log.info("Creating user in Keycloak: {}", request.getUsername());
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        
        try {
            var response = identityClient.createUser(
                    request,
                    KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken(),
                    adminInfo.adminPath
            );
            
            if (response.getStatusCode().value() != 201) {
                throw new KeycloakException("Failed to create user in Keycloak. Status: " + 
                        response.getStatusCode());
            }
            
            String location = response.getHeaders().getFirst("Location");
            if (location == null) {
                throw new KeycloakException("Keycloak did not return user ID in Location header.");
            }
            String keycloakUserId = location.substring(location.lastIndexOf('/') + 1);
            log.info("User created in Keycloak with userId: {}", keycloakUserId);
            return keycloakUserId;
        } catch (FeignException e) {
            log.error("Failed to create user in Keycloak: {} - {}", e.status(), e.contentUTF8());
            throw new KeycloakException("Failed to create user in Keycloak: " + e.contentUTF8(), e);
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak: {}", e.getMessage());
            throw new KeycloakException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }
    }
    
    /**
     * Assign role to user in Keycloak (realm role or client role)
     */
    public void assignRoleToUser(String userId, String roleName) {
        log.info("Assigning role {} to user {} in Keycloak", roleName, userId);
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        String authHeader = KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken();
        
        try {
            // Try to assign as realm role first
            try {
                List<Map<String, Object>> realmRoles = identityClient.getRealmRoles(adminInfo.adminPath, authHeader);
                Map<String, Object> roleToAssign = realmRoles.stream()
                        .filter(role -> roleName.equalsIgnoreCase(role.get("name").toString()))
                        .findFirst()
                        .orElse(null);
                
                if (roleToAssign != null) {
                    List<Map<String, Object>> rolesToAssign = List.of(roleToAssign);
                    identityClient.assignRealmRole(adminInfo.adminPath, userId, rolesToAssign, authHeader);
                    log.info("Successfully assigned realm role {} to user {}", roleName, userId);
                    return;
                }
            } catch (Exception e) {
                log.debug("Role {} not found as realm role, trying client role: {}", roleName, e.getMessage());
            }
            
            // Try to assign as client role
            try {
                // Get client ID for project3
                List<Map<String, Object>> clients = identityClient.getClients(
                        adminInfo.adminPath, 
                        keycloakProperties.getClientId(), 
                        authHeader);
                
                if (clients == null || clients.isEmpty()) {
                    log.warn("Client {} not found in Keycloak", keycloakProperties.getClientId());
                    return;
                }
                
                String clientUuid = clients.get(0).get("id").toString();
                List<Map<String, Object>> clientRoles = identityClient.getClientRoles(
                        adminInfo.adminPath, 
                        clientUuid, 
                        authHeader);
                
                Map<String, Object> roleToAssign = clientRoles.stream()
                        .filter(role -> roleName.equalsIgnoreCase(role.get("name").toString()))
                        .findFirst()
                        .orElse(null);
                
                if (roleToAssign != null) {
                    List<Map<String, Object>> rolesToAssign = List.of(roleToAssign);
                    identityClient.assignClientRole(adminInfo.adminPath, userId, clientUuid, rolesToAssign, authHeader);
                    log.info("Successfully assigned client role {} to user {}", roleName, userId);
                    return;
                }
            } catch (Exception e) {
                log.warn("Failed to assign client role {} to user {}: {}", roleName, userId, e.getMessage());
            }
            
            log.warn("Role {} not found in Keycloak realm or client roles. User created but role not assigned.", roleName);
        } catch (Exception e) {
            log.error("Failed to assign role {} to user {}: {}", roleName, userId, e.getMessage());
            // Don't throw exception - user is created, role assignment is best effort
        }
    }
    
    /**
     * Create realm role in Keycloak
     */
    public void createRealmRole(String roleName, String description) {
        log.info("Creating realm role {} in Keycloak", roleName);
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        String authHeader = KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken();
        
        try {
            Map<String, Object> role = new HashMap<>();
            role.put("name", roleName);
            if (description != null && !description.isEmpty()) {
                role.put("description", description);
            }
            
            identityClient.createRealmRole(adminInfo.adminPath, role, authHeader);
            log.info("Successfully created realm role {}", roleName);
        } catch (FeignException e) {
            if (e.status() == 409) {
                log.info("Realm role {} already exists", roleName);
            } else {
                log.error("Failed to create realm role {}: {} - {}", roleName, e.status(), e.contentUTF8());
                throw new KeycloakException("Failed to create realm role: " + e.contentUTF8(), e);
            }
        } catch (Exception e) {
            log.error("Failed to create realm role {}: {}", roleName, e.getMessage());
            throw new KeycloakException("Failed to create realm role: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create client role in Keycloak
     */
    public void createClientRole(String roleName, String description) {
        log.info("Creating client role {} in Keycloak", roleName);
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        String authHeader = KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken();
        
        try {
            // Get client ID for project3
            List<Map<String, Object>> clients = identityClient.getClients(
                    adminInfo.adminPath, 
                    keycloakProperties.getClientId(), 
                    authHeader);
            
            if (clients == null || clients.isEmpty()) {
                throw new KeycloakException("Client " + keycloakProperties.getClientId() + " not found in Keycloak");
            }
            
            String clientUuid = clients.get(0).get("id").toString();
            Map<String, Object> role = new HashMap<>();
            role.put("name", roleName);
            if (description != null && !description.isEmpty()) {
                role.put("description", description);
            }
            
            identityClient.createClientRole(adminInfo.adminPath, clientUuid, role, authHeader);
            log.info("Successfully created client role {} for client {}", roleName, keycloakProperties.getClientId());
        } catch (FeignException e) {
            if (e.status() == 409) {
                log.info("Client role {} already exists", roleName);
            } else {
                log.error("Failed to create client role {}: {} - {}", roleName, e.status(), e.contentUTF8());
                throw new KeycloakException("Failed to create client role: " + e.contentUTF8(), e);
            }
        } catch (Exception e) {
            log.error("Failed to create client role {}: {}", roleName, e.getMessage());
            throw new KeycloakException("Failed to create client role: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all realm roles from Keycloak
     */
    public List<Map<String, Object>> getRealmRoles() {
        log.info("Getting realm roles from Keycloak");
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        String authHeader = KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken();
        
        try {
            List<Map<String, Object>> roles = identityClient.getRealmRoles(adminInfo.adminPath, authHeader);
            log.info("Retrieved {} realm roles", roles != null ? roles.size() : 0);
            return roles != null ? roles : List.of();
        } catch (Exception e) {
            log.error("Failed to get realm roles: {}", e.getMessage());
            throw new KeycloakException("Failed to get realm roles: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all client roles from Keycloak for project3 client
     */
    public List<Map<String, Object>> getClientRoles() {
        log.info("Getting client roles from Keycloak for client {}", keycloakProperties.getClientId());
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        String authHeader = KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken();
        
        try {
            List<Map<String, Object>> clients = identityClient.getClients(
                    adminInfo.adminPath, 
                    keycloakProperties.getClientId(), 
                    authHeader);
            
            if (clients == null || clients.isEmpty()) {
                log.warn("Client {} not found in Keycloak", keycloakProperties.getClientId());
                return List.of();
            }
            
            String clientUuid = clients.get(0).get("id").toString();
            List<Map<String, Object>> roles = identityClient.getClientRoles(adminInfo.adminPath, clientUuid, authHeader);
            log.info("Retrieved {} client roles for client {}", roles != null ? roles.size() : 0, keycloakProperties.getClientId());
            return roles != null ? roles : List.of();
        } catch (Exception e) {
            log.error("Failed to get client roles: {}", e.getMessage());
            throw new KeycloakException("Failed to get client roles: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize all required roles in Keycloak (realm roles or client roles)
     * This should be called once during application startup or via admin endpoint
     */
    public void initializeRoles() {
        log.info("Initializing required roles in Keycloak");
        String[] roles = {"CUSTOMER", "STAFF", "WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN"};
        String[] descriptions = {
            "Khách hàng",
            "Nhân viên phục vụ",
            "Nhân viên kho",
            "Quản lý nhà hàng",
            "Quản trị viên hệ thống"
        };
        
        // Try to create as client roles first (preferred for application-specific roles)
        boolean useClientRoles = true;
        try {
            List<Map<String, Object>> clients = identityClient.getClients(
                    getKeycloakAdminInfo().adminPath,
                    keycloakProperties.getClientId(),
                    KeycloakConstants.BEARER_PREFIX + getKeycloakAdminInfo().token.getAccessToken());
            if (clients == null || clients.isEmpty()) {
                useClientRoles = false;
                log.warn("Client {} not found, will create as realm roles", keycloakProperties.getClientId());
            }
        } catch (Exception e) {
            useClientRoles = false;
            log.warn("Failed to check client, will create as realm roles: {}", e.getMessage());
        }
        
        for (int i = 0; i < roles.length; i++) {
            boolean roleCreated = false;
            if (useClientRoles) {
                try {
                    createClientRole(roles[i], descriptions[i]);
                    roleCreated = true;
                } catch (FeignException e) {
                    if (e.status() == 403 || e.status() == 401) {
                        // Permission denied - fallback to realm role
                        log.warn("Failed to create client role {} due to permission issue ({}), falling back to realm role", roles[i], e.status());
                        try {
                            createRealmRole(roles[i], descriptions[i]);
                            roleCreated = true;
                        } catch (Exception ex) {
                            log.error("Failed to create realm role {}: {}", roles[i], ex.getMessage());
                        }
                    } else if (e.status() == 409) {
                        // Role already exists
                        log.info("Client role {} already exists", roles[i]);
                        roleCreated = true;
                    } else {
                        log.error("Failed to create client role {}: {} - {}", roles[i], e.status(), e.contentUTF8());
                    }
                } catch (Exception e) {
                    log.error("Failed to create client role {}: {}", roles[i], e.getMessage());
                    // Try realm role as fallback
                    try {
                        log.info("Attempting to create realm role {} as fallback", roles[i]);
                        createRealmRole(roles[i], descriptions[i]);
                        roleCreated = true;
                    } catch (Exception ex) {
                        log.error("Failed to create realm role {}: {}", roles[i], ex.getMessage());
                    }
                }
            } else {
                try {
                    createRealmRole(roles[i], descriptions[i]);
                    roleCreated = true;
                } catch (Exception e) {
                    log.error("Failed to initialize role {}: {}", roles[i], e.getMessage());
                }
            }
            
            if (!roleCreated) {
                log.warn("Role {} was not created. Please check Keycloak permissions or create manually.", roles[i]);
            }
        }
        
        log.info("Role initialization completed");
    }
    
    /**
     * Update user in Keycloak
     */
    public void updateUser(String userId, UserCreationRequest request) {
        log.info("Updating user {} in Keycloak.", userId);
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        
        try {
            identityClient.updateUser(
                    adminInfo.adminPath,
                    userId,
                    request,
                    KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken()
            );
            log.info("User {} updated successfully in Keycloak.", userId);
        } catch (FeignException e) {
            log.error("Failed to update user {} in Keycloak: {} - {}", userId, e.status(), e.contentUTF8());
            throw new KeycloakException("Failed to update user in Keycloak: " + e.contentUTF8(), e);
        } catch (Exception e) {
            log.error("Failed to update user {} in Keycloak: {}", userId, e.getMessage());
            throw new KeycloakException("Failed to update user in Keycloak: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete user from Keycloak
     */
    public void deleteUser(String userId) {
        log.info("Deleting user {} from Keycloak.", userId);
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        
        try {
            identityClient.deleteUser(
                    adminInfo.adminPath,
                    userId,
                    KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken()
            );
            log.info("User {} deleted successfully from Keycloak.", userId);
        } catch (FeignException e) {
            log.error("Failed to delete user {} from Keycloak: {} - {}", userId, e.status(), e.contentUTF8());
            throw new KeycloakException("Failed to delete user from Keycloak: " + e.contentUTF8(), e);
        } catch (Exception e) {
            log.error("Failed to delete user {} from Keycloak: {}", userId, e.getMessage());
            throw new KeycloakException("Failed to delete user from Keycloak: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get user from Keycloak
     */
    public Map<String, Object> getUser(String userId) {
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        try {
            return identityClient.getUser(
                    adminInfo.adminPath,
                    userId,
                    KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken()
            );
        } catch (Exception e) {
            log.error("Failed to get user {} from Keycloak: {}", userId, e.getMessage());
            throw new KeycloakException("Failed to get user from Keycloak: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if user exists in Keycloak
     */
    public boolean userExistsInKeycloak(String userId) {
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        try {
            identityClient.getUser(
                    adminInfo.adminPath,
                    userId,
                    KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken()
            );
            return true;
        } catch (FeignException e) {
            if (e.status() == 404) {
                return false;
            }
            log.warn("Error checking if user {} exists in Keycloak: {} - {}", userId, e.status(), e.contentUTF8());
            // If error, assume user exists to avoid accidental deletion
            return true;
        } catch (Exception e) {
            log.warn("Error checking if user {} exists in Keycloak: {}", userId, e.getMessage());
            // If error, assume user exists to avoid accidental deletion
            return true;
        }
    }
    
    /**
     * Enable user in Keycloak
     */
    public void enableUser(String userId) {
        updateUserEnabled(userId, true);
    }
    
    /**
     * Disable user in Keycloak
     */
    public void disableUser(String userId) {
        updateUserEnabled(userId, false);
    }
    
    /**
     * Reset password in Keycloak
     */
    public void resetPassword(String userId, String newPassword) {
        log.info("Resetting password for user {} in Keycloak.", userId);
        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        
        try {
            identityClient.resetPassword(
                    adminInfo.adminPath,
                    userId,
                    PasswordResetRequest.builder()
                            .type("password")
                            .value(newPassword)
                            .temporary(false)
                            .build(),
                    KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken()
            );
            log.info("Password reset successfully for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to reset password for user {}: {}", userId, e.getMessage());
            throw new KeycloakException("Failed to reset password: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send verification email to user
     */
    public void sendVerificationEmail(String userId, String email) {
        try {
            // Ensure SMTP is configured
            configureRealmSMTP(false);
            
            KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
            String authHeader = KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken();
            List<String> actions = List.of(KeycloakConstants.VERIFY_EMAIL_ACTION);
            
            // Try PUT method first
            try {
                identityClient.executeActionsEmail(adminInfo.adminPath, userId,
                        authHeader, null, null, actions);
                log.info("Successfully triggered Keycloak verify email for {} using PUT", email);
                return;
            } catch (Exception e1) {
                log.warn("execute-actions-email PUT without client_id failed: {}", e1.getMessage());
            }
            
            // Try PUT with client_id
            for (String clientId : List.of("account", "account-console")) {
                try {
                    identityClient.executeActionsEmail(adminInfo.adminPath, userId,
                            authHeader, clientId, null, actions);
                    log.info("Successfully triggered Keycloak verify email for {} using PUT (client_id={})", email, clientId);
                    return;
                } catch (Exception e2) {
                    log.warn("execute-actions-email PUT with client_id={} failed: {}", clientId, e2.getMessage());
                }
            }
            
            log.warn("All attempts to send Keycloak verify email failed for {}. " +
                    "User was created with requiredActions=[VERIFY_EMAIL], so Keycloak may send email automatically when user first logs in.", email);
        } catch (Exception ex) {
            log.error("Failed to send Keycloak verify email for {}. Error: {}", email, ex.getMessage(), ex);
        }
    }
    
    /**
     * Configure SMTP settings for Keycloak realm
     */
    public void configureRealmSMTP(boolean forceUpdate) {
        if (smtpConfigured && !forceUpdate) {
            return;
        }
        
        try {
            KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
            
            // Get current realm configuration
            Map<String, Object> currentRealm;
            try {
                currentRealm = identityClient.getRealm(adminInfo.adminPath, 
                        KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken());
            } catch (Exception e) {
                log.warn("Failed to get current realm config: {}. SMTP may need to be configured manually in Keycloak Admin Console.", e.getMessage());
                return;
            }
            
            if (currentRealm == null) {
                log.warn("Failed to get realm configuration. SMTP may need to be configured manually.");
                return;
            }
            
            // Check if SMTP is already configured
            Object existingSmtp = currentRealm.get("smtpServer");
            if (existingSmtp != null && existingSmtp instanceof Map) {
                Map<?, ?> existingSmtpMap = (Map<?, ?>) existingSmtp;
                Object host = existingSmtpMap.get("host");
                if (host != null && !host.toString().isEmpty() && !forceUpdate) {
                    log.info("SMTP already configured in Keycloak realm: {}. Current config: host={}, port={}, from={}", 
                            keycloakProperties.getRealm(), host, existingSmtpMap.get("port"), existingSmtpMap.get("from"));
                    smtpConfigured = true;
                    return;
                }
            }
            
            // Build SMTP configuration
            Map<String, Object> smtpConfig = new HashMap<>();
            KeycloakProperties.SmtpProperties smtp = keycloakProperties.getSmtp();
            smtpConfig.put("host", smtp.getHost());
            smtpConfig.put("port", smtp.getPort());
            smtpConfig.put("from", smtp.getFrom());
            smtpConfig.put("fromDisplayName", smtp.getFromDisplayName());
            smtpConfig.put("auth", String.valueOf(smtp.isAuth()));
            smtpConfig.put("ssl", String.valueOf(smtp.isSsl()));
            smtpConfig.put("starttls", String.valueOf(smtp.isStarttls()));
            
            if (smtp.isAuth()) {
                smtpConfig.put("user", smtp.getUser());
                smtpConfig.put("password", smtp.getPassword());
            }
            
            log.info("Configuring SMTP for Keycloak realm: host={}, port={}, from={}, user={}", 
                    smtp.getHost(), smtp.getPort(), smtp.getFrom(), smtp.getUser());
            
            // Update realm with SMTP config
            currentRealm.put("smtpServer", smtpConfig);
            
            // Update realm
            try {
                identityClient.updateRealm(adminInfo.adminPath, currentRealm,
                        KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken());
                log.info("Successfully configured SMTP for Keycloak realm: {}", keycloakProperties.getRealm());
                smtpConfigured = true;
            } catch (Exception e) {
                log.error("Failed to update realm SMTP config: {}. Please configure SMTP manually in Keycloak Admin Console: Realm Settings > Email", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to configure Keycloak realm SMTP: {}. Please configure SMTP manually in Keycloak Admin Console: Realm Settings > Email", e.getMessage());
        }
    }
    
    private void updateUserEnabled(String userId, boolean enabled) {
        try {
            KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
            
            UserCreationRequest updateRequest = UserCreationRequest.builder()
                    .enabled(enabled)
                    .build();
            
            identityClient.updateUser(adminInfo.adminPath, userId, updateRequest, 
                    KeycloakConstants.BEARER_PREFIX + adminInfo.token.getAccessToken());
            log.info("User {} in Keycloak: {}", enabled ? "enabled" : "disabled", userId);
        } catch (Exception e) {
            log.error("Failed to {} user in Keycloak: {}. Error: {}", 
                    enabled ? "enable" : "disable", userId, e.getMessage());
            throw new KeycloakException("Failed to " + (enabled ? "enable" : "disable") + " user in Keycloak", e);
        }
    }
    
    private String[] getRealmPaths() {
        return new String[]{
                KeycloakConstants.REALMS_PATH + keycloakProperties.getRealm(), 
                KeycloakConstants.AUTH_REALMS_PATH + keycloakProperties.getRealm()
        };
    }
}

