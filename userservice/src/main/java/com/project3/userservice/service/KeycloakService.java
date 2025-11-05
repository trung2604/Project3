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
                info.token = identityClient.exchangeClientToken(
                        TokenExchangeRequest.builder()
                                .grant_type(KeycloakConstants.GRANT_TYPE_CLIENT_CREDENTIALS)
                                .client_secret(keycloakProperties.getClientSecret())
                                .client_id(keycloakProperties.getClientId())
                                .scope(KeycloakConstants.SCOPE_OPENID)
                                .build(),
                        realmPath
                );
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
                TokenExchangeResponse tokenResponse = identityClient.loginUser(
                        UserLoginRequest.builder()
                                .grant_type(KeycloakConstants.GRANT_TYPE_PASSWORD)
                                .client_id(keycloakProperties.getClientId())
                                .client_secret(keycloakProperties.getClientSecret())
                                .username(username)
                                .password(password)
                                .scope(String.join(" ", KeycloakConstants.SCOPE_OPENID, 
                                        KeycloakConstants.SCOPE_PROFILE, KeycloakConstants.SCOPE_EMAIL))
                                .build(),
                        realmPath
                );
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
            TokenExchangeResponse token = identityClient.exchangeClientToken(
                    TokenExchangeRequest.builder()
                            .grant_type(KeycloakConstants.GRANT_TYPE_AUTHORIZATION_CODE)
                            .client_id(keycloakProperties.getClientId())
                            .client_secret(keycloakProperties.getClientSecret())
                            .code(code)
                            .redirect_uri(redirectUri)
                            .scope(String.join(" ", KeycloakConstants.SCOPE_OPENID, 
                                    KeycloakConstants.SCOPE_PROFILE, KeycloakConstants.SCOPE_EMAIL))
                            .build(),
                    realmPath
            );
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

