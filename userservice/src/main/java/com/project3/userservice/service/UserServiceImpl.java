package com.project3.userservice.service;

import com.project3.userservice.dto.CreateUserRequestDTO;
import com.project3.userservice.dto.LoginRequestDTO;
import com.project3.userservice.dto.LoginResponseDTO;
import com.project3.userservice.dto.PagedUserResponseDTO;
import com.project3.userservice.dto.RegisterUserRequestDTO;
import com.project3.userservice.dto.UpdateUserRequestDTO;
import com.project3.userservice.dto.UserResponseDTO;
import com.project3.userservice.dto.identity.TokenExchangeRequest;
import com.project3.userservice.dto.identity.TokenExchangeResponse;
import com.project3.userservice.dto.identity.UserCreationRequest;
import com.project3.userservice.dto.identity.UserLoginRequest;
import com.project3.userservice.entity.User;
import com.project3.userservice.repository.IdentityClient;
import com.project3.userservice.repository.UserRepository;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdentityClient identityClient;

    @Autowired
    private CloudinaryService cloudinaryService;

    // Removed custom email verification components

    @Value("${idp.client-id}")
    @NonFinal
    private String clientId;

    @Value("${idp.client-secret}")
    @NonFinal
    private String clientSecret;

    @Value("${idp.url}")
    @NonFinal
    private String keycloakUrl;

    @Value("${idp.realm}")
    @NonFinal
    private String realm;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Attempting login for username: {}", request.getUsername());
        
        User user = userRepository.findByUsername(request.getUsername())
                .orElseGet(() -> {
                    log.warn("User not found in database for username: {}", request.getUsername());
                    return null;
                });
        
        if (user == null) {
            throw new RuntimeException("Invalid username or password");
        }
        
        log.info("User found in database. Status: {}", user.getStatus());
        
        // Try to authenticate with Keycloak first
        TokenExchangeResponse tokenResponse = authenticateUserWithKeycloak(
                request.getUsername(), request.getPassword());
        
        if (tokenResponse == null) {
            throw new RuntimeException("Invalid username or password. Please verify your credentials or contact administrator if account is inactive.");
        }

        // If user is INACTIVE, check if email is verified in Keycloak
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            try {
                KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
                if (adminInfo != null && adminInfo.token != null && adminInfo.adminPath != null) {
                    var kcUser = identityClient.getUser(adminInfo.adminPath, user.getUserId(),
                            "Bearer " + adminInfo.token.getAccessToken());
                    Object emailVerifiedObj = kcUser.get("emailVerified");
                    Boolean emailVerified = emailVerifiedObj instanceof Boolean ? (Boolean) emailVerifiedObj : 
                                           (emailVerifiedObj instanceof String && "true".equalsIgnoreCase(emailVerifiedObj.toString()));
                    
                    if (Boolean.TRUE.equals(emailVerified)) {
                        log.info("Email verified in Keycloak for user: {}. Activating user.", request.getUsername());
                        user.setStatus(User.UserStatus.ACTIVE);
                        userRepository.save(user);
                        // Also enable user in Keycloak if not already enabled
                        Object enabledObj = kcUser.get("enabled");
                        Boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : 
                                         (enabledObj instanceof String && "true".equalsIgnoreCase(enabledObj.toString()));
                        if (!Boolean.TRUE.equals(enabled)) {
                            enableUserInKeycloak(user.getUserId());
                        }
                    } else {
                        log.warn("Login attempt for inactive user with unverified email: {}. Status: {}", request.getUsername(), user.getStatus());
                        throw new RuntimeException("User account is not active. Please verify your email before logging in.");
                    }
                } else {
                    log.warn("Cannot check email verification status. User status: {}", user.getStatus());
                    throw new RuntimeException("User account is not active. Please verify your email.");
                }
            } catch (Exception e) {
                log.error("Failed to check email verification status: {}", e.getMessage());
                throw new RuntimeException("User account is not active. Please verify your email.");
            }
        }

        LoginResponseDTO response = buildLoginResponse(tokenResponse, user);
        return response;
    }

    @Override
    public UserResponseDTO registerUser(RegisterUserRequestDTO request) {
        CreateUserRequestDTO createDto = new CreateUserRequestDTO();
        createDto.setEmail(request.getEmail());
        createDto.setUsername(request.getUsername());
        createDto.setPassword(request.getPassword());
        createDto.setFirstName(request.getFirstName());
        createDto.setLastName(request.getLastName());
        createDto.setPhone(request.getPhone());
        createDto.setAddress(request.getAddress());
        createDto.setAvatarUrl(request.getAvatarUrl());
        createDto.setAvatarPublicId(request.getAvatarPublicId());
        createDto.setDateOfBirth(request.getDateOfBirth());
        // Always assign CUSTOMER role for self-registered users
        createDto.setRole(User.UserRole.CUSTOMER);

        return createUser(createDto);
    }

    @Override
    public UserResponseDTO createUser(CreateUserRequestDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
        if (adminInfo == null || adminInfo.token == null || adminInfo.adminPath == null) {
            log.error("Failed to get admin token from Keycloak");
            throw new RuntimeException("Failed to authenticate with Keycloak. " +
                    "Please verify:\n" +
                    "1. Keycloak server is running on " + keycloakUrl + "\n" +
                    "2. Realm '" + realm + "' exists in Keycloak\n" +
                    "3. Client credentials are correct (client-id: " + clientId + ")\n" +
                    "4. Keycloak version and URL pattern (tried /realms/ and /auth/realms/)");
        }
        
        TokenExchangeResponse tokenResponse = adminInfo.token;

        UserCreationRequest keycloakUserRequest = buildKeycloakUserRequest(request);

        ResponseEntity<Void> creationResponse;
        try {
            creationResponse = identityClient.createUser(
                    keycloakUserRequest,
                    "Bearer " + tokenResponse.getAccessToken(),
                    adminInfo.adminPath
            );
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak", e);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage());
        }
        
        if (creationResponse.getStatusCode().value() != 201) {
            log.error("Keycloak returned unexpected status: {}", creationResponse.getStatusCode());
            throw new RuntimeException("Failed to create user in Keycloak. Status: " + 
                    creationResponse.getStatusCode());
        }

        String userId = extractUserId(creationResponse);
        log.info("User created in Keycloak with userId: {}", userId);

        User user = buildUserEntity(userId, request);

        User savedUser = userRepository.save(user);
        // Trigger Keycloak to send verification email
        sendKeycloakVerifyEmail(savedUser.getUserId(), savedUser.getEmail());
        return UserResponseDTO.fromEntity(savedUser);
    }

    @Override
    public UserResponseDTO updateUser(String userId, UpdateUserRequestDTO request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
            // If email changed and user inactive, trigger Keycloak verify email again
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                sendKeycloakVerifyEmail(user.getUserId(), user.getEmail());
            }
        }

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getRole() != null) user.setRole(request.getRole());
        
        User.UserStatus oldStatus = user.getStatus();
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
            if (request.getStatus() == User.UserStatus.ACTIVE && oldStatus != User.UserStatus.ACTIVE) {
                enableUserInKeycloak(user.getUserId());
            } else if (request.getStatus() != User.UserStatus.ACTIVE && oldStatus == User.UserStatus.ACTIVE) {
                disableUserInKeycloak(user.getUserId());
            }
        }
        
        if (isNotBlank(request.getAvatarUrl()) || isNotBlank(request.getAvatarPublicId())) {
            if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isEmpty()) {
                log.info("Deleting old avatar with publicId: {} for user: {}", user.getAvatarPublicId(), userId);
                boolean deleted = cloudinaryService.deleteImage(user.getAvatarPublicId());
                if (!deleted) {
                    log.warn("Failed to delete old avatar from Cloudinary, but continuing with update");
                }
            }
            if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
            if (request.getAvatarPublicId() != null) user.setAvatarPublicId(request.getAvatarPublicId());
        }
        
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());

        User updatedUser = userRepository.save(user);
        return UserResponseDTO.fromEntity(updatedUser);
    }

    @Override
    public void deleteUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));
        
        // Delete user in Keycloak first
        try {
            KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
            if (adminInfo == null || adminInfo.token == null || adminInfo.adminPath == null) {
                log.error("Failed to get admin token or path to delete user in Keycloak");
            } else {
                identityClient.deleteUser(adminInfo.adminPath, user.getUserId(),
                        "Bearer " + adminInfo.token.getAccessToken());
                log.info("Deleted user in Keycloak: {}", user.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to delete user in Keycloak: {}. Error: {}", user.getUserId(), e.getMessage());
        }

        if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isEmpty()) {
            log.info("Deleting avatar with publicId: {} for user: {}", user.getAvatarPublicId(), userId);
            boolean deleted = cloudinaryService.deleteImage(user.getAvatarPublicId());
            if (!deleted) {
                log.warn("Failed to delete avatar from Cloudinary for user: {}, but continuing with user deletion", userId);
            }
        }
        
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));
        return UserResponseDTO.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return UserResponseDTO.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedUserResponseDTO getAllUsers(Integer page, Integer size, String search, User.UserRole role, User.UserStatus status) {
        int pageNumber = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? size : 20;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (isNotBlank(search)) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            Specification<User> searchSpec = (root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(root.get("username")), searchPattern),
                    cb.like(cb.lower(root.get("firstName")), searchPattern),
                    cb.like(cb.lower(root.get("lastName")), searchPattern),
                    cb.like(cb.lower(root.get("phone")), searchPattern)
                );
            spec = spec.and(searchSpec);
        }

        if (role != null) {
            Specification<User> roleSpec = (root, query, cb) -> cb.equal(root.get("role"), role);
            spec = spec.and(roleSpec);
        }

        if (status != null) {
            Specification<User> statusSpec = (root, query, cb) -> cb.equal(root.get("status"), status);
            spec = spec.and(statusSpec);
        }

        Page<User> userPage = userRepository.findAll(spec, pageable);
        
        List<UserResponseDTO> userDTOs = userPage.getContent().stream()
                .map(UserResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return new PagedUserResponseDTO(
                userDTOs,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @Override
    public UserResponseDTO toggleUserStatus(String userId, User.UserStatus status) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));
        
        User.UserStatus oldStatus = user.getStatus();
        user.setStatus(status);
        
        if (status == User.UserStatus.ACTIVE && oldStatus != User.UserStatus.ACTIVE) {
            enableUserInKeycloak(user.getUserId());
        } else if (status != User.UserStatus.ACTIVE && oldStatus == User.UserStatus.ACTIVE) {
            disableUserInKeycloak(user.getUserId());
        }
        
        User updatedUser = userRepository.save(user);
        return UserResponseDTO.fromEntity(updatedUser);
    }

    // Custom token verification flow removed; handled by Keycloak email verification

    private void updateUserEnabledInKeycloak(String keycloakUserId, boolean enabled) {
        try {
            KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
            if (adminInfo == null || adminInfo.token == null || adminInfo.adminPath == null) {
                log.error("Failed to get admin token or path to update user in Keycloak");
                return;
            }
            
            UserCreationRequest updateRequest = UserCreationRequest.builder()
                    .enabled(enabled)
                    .build();
            
            identityClient.updateUser(adminInfo.adminPath, keycloakUserId, updateRequest, 
                    "Bearer " + adminInfo.token.getAccessToken());
            log.info("User {} in Keycloak: {}", enabled ? "enabled" : "disabled", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to {} user in Keycloak: {}. Error: {}", 
                    enabled ? "enable" : "disable", keycloakUserId, e.getMessage());
        }
    }

    private void enableUserInKeycloak(String keycloakUserId) {
        updateUserEnabledInKeycloak(keycloakUserId, true);
    }

    private void disableUserInKeycloak(String keycloakUserId) {
        updateUserEnabledInKeycloak(keycloakUserId, false);
    }

    private static class KeycloakAdminInfo {
        TokenExchangeResponse token;
        String adminPath;
    }

    private String[] getRealmPaths() {
        return new String[]{"/realms/" + realm, "/auth/realms/" + realm};
    }

    private KeycloakAdminInfo getKeycloakAdminInfo() {
        String[] realmPaths = getRealmPaths();
        KeycloakAdminInfo info = new KeycloakAdminInfo();
        
        for (String realmPath : realmPaths) {
            try {
                info.token = identityClient.exchangeClientToken(
                        TokenExchangeRequest.builder()
                                .grant_type("client_credentials")
                                .client_secret(clientSecret)
                                .client_id(clientId)
                                .scope("openid")
                                .build(),
                        realmPath
                );
                String baseRealmPath = realmPath.startsWith("/auth/") ? "/auth" : "";
                info.adminPath = baseRealmPath + "/admin/realms/" + realm;
                return info;
            } catch (Exception e) {
                log.warn("Failed to get admin token with path {}: {}", realmPath, e.getMessage());
            }
        }
        return null;
    }

    private UserCreationRequest buildKeycloakUserRequest(CreateUserRequestDTO request) {
        UserCreationRequest keycloakUserRequest = UserCreationRequest.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .enabled(true)
                .emailVerified(false)
                .requiredActions(java.util.List.of("VERIFY_EMAIL"))
                .build();
        
        if (isNotBlank(request.getLastName())) {
            keycloakUserRequest.setLastName(request.getLastName());
        }
        
        keycloakUserRequest.setPassword(request.getPassword());
        if (isNotBlank(request.getPhone())) {
            keycloakUserRequest.setPhone(request.getPhone());
        }
        if (isNotBlank(request.getAddress())) {
            keycloakUserRequest.setAddress(request.getAddress());
        }
        keycloakUserRequest.setRole(request.getRole().name());
        if (isNotBlank(request.getAvatarUrl())) {
            keycloakUserRequest.setAvatarUrl(request.getAvatarUrl());
        }
        if (isNotBlank(request.getAvatarPublicId())) {
            keycloakUserRequest.setAvatarPublicId(request.getAvatarPublicId());
        }
        if (request.getDateOfBirth() != null) {
            keycloakUserRequest.setDateOfBirth(request.getDateOfBirth());
        }
        
        return keycloakUserRequest;
    }

    private User buildUserEntity(String userId, CreateUserRequestDTO request) {
        User.UserBuilder userBuilder = User.builder()
                .userId(userId)
                .email(request.getEmail())
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .role(request.getRole())
                .status(User.UserStatus.INACTIVE);
        
        if (isNotBlank(request.getLastName())) {
            userBuilder.lastName(request.getLastName());
        }
        if (isNotBlank(request.getPhone())) {
            userBuilder.phone(request.getPhone());
        }
        if (isNotBlank(request.getAddress())) {
            userBuilder.address(request.getAddress());
        }
        if (isNotBlank(request.getAvatarUrl())) {
            userBuilder.avatarUrl(request.getAvatarUrl());
        }
        if (isNotBlank(request.getAvatarPublicId())) {
            userBuilder.avatarPublicId(request.getAvatarPublicId());
        }
        if (request.getDateOfBirth() != null) {
            userBuilder.dateOfBirth(request.getDateOfBirth());
        }
        
        return userBuilder.build();
    }

    private TokenExchangeResponse authenticateUserWithKeycloak(String username, String password) {
        String[] realmPaths = getRealmPaths();
        Exception lastException = null;
        
        for (String realmPath : realmPaths) {
            try {
                log.info("Attempting login with Keycloak for username: {} at {}{}", 
                        username, keycloakUrl, realmPath);
                TokenExchangeResponse tokenResponse = identityClient.loginUser(
                        UserLoginRequest.builder()
                                .grant_type("password")
                                .client_id(clientId)
                                .client_secret(clientSecret)
                                .username(username)
                                .password(password)
                                .scope("openid profile email")
                                .build(),
                        realmPath
                );
                log.info("Login successful for username: {}", username);
                return tokenResponse;
            } catch (Exception e) {
                String errorDetails = e.getClass().getSimpleName() + ": " + e.getMessage();
                if (e instanceof feign.FeignException) {
                    feign.FeignException feignEx = (feign.FeignException) e;
                    errorDetails = "Keycloak returned " + feignEx.status() + " - " + feignEx.contentUTF8();
                    log.error("Keycloak login error details: {}", errorDetails);
                }
                log.warn("Failed to login with path {}: {}", realmPath, errorDetails);
                lastException = e;
            }
        }
        
        log.error("Login failed for username: {}. Last error: {}", 
                username, lastException != null ? lastException.getMessage() : "Unknown");
        if (lastException != null) {
            log.error("Full exception details:", lastException);
        }
        return null;
    }

    private LoginResponseDTO buildLoginResponse(TokenExchangeResponse tokenResponse, User user) {
        LoginResponseDTO response = new LoginResponseDTO();
        response.setAccessToken(tokenResponse.getAccessToken());
        response.setRefreshToken(tokenResponse.getRefreshToken() != null ? 
                tokenResponse.getRefreshToken() : tokenResponse.getIdToken());
        response.setTokenType(tokenResponse.getTokenType());
        response.setExpiresIn(parseLongSafely(tokenResponse.getExpiresIn(), 300L));
        response.setRefreshExpiresIn(parseLongSafely(tokenResponse.getRefreshExpiresIn(), 1800L));
        response.setUser(UserResponseDTO.fromEntity(user));
        return response;
    }

    private Long parseLongSafely(String value, Long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse {}: {}", value, e.getMessage());
            return defaultValue;
        }
    }

    private void sendKeycloakVerifyEmail(String keycloakUserId, String email) {
        try {
            KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
            if (adminInfo == null || adminInfo.token == null || adminInfo.adminPath == null) {
                log.error("Failed to get admin token or path to send verify email via Keycloak");
                return;
            }

            // Verify user exists and check requiredActions
            try {
                var kcUser = identityClient.getUser(adminInfo.adminPath, keycloakUserId,
                        "Bearer " + adminInfo.token.getAccessToken());
                Object requiredActionsObj = kcUser.get("requiredActions");
                log.info("User requiredActions in Keycloak: {}", requiredActionsObj);
                
                // If requiredActions is empty or null, update user to add VERIFY_EMAIL
                if (requiredActionsObj == null || (requiredActionsObj instanceof List && ((List<?>) requiredActionsObj).isEmpty())) {
                    log.info("Updating user to add VERIFY_EMAIL required action");
                    UserCreationRequest updateRequest = UserCreationRequest.builder()
                            .requiredActions(java.util.List.of("VERIFY_EMAIL"))
                            .build();
                    identityClient.updateUser(adminInfo.adminPath, keycloakUserId, updateRequest,
                            "Bearer " + adminInfo.token.getAccessToken());
                }
            } catch (Exception verifyEx) {
                log.warn("Failed to verify/update user in Keycloak: {}", verifyEx.getMessage());
            }

            java.util.List<String> actions = java.util.List.of("VERIFY_EMAIL");
            String redirectUri = System.getProperty("app.verify-redirect-uri", null);
            String authHeader = "Bearer " + adminInfo.token.getAccessToken();

            // Try PUT method first (some Keycloak versions prefer PUT)
            try {
                identityClient.executeActionsEmail(adminInfo.adminPath, keycloakUserId,
                        authHeader, null, redirectUri, actions);
                log.info("Successfully triggered Keycloak verify email for {} using PUT (no client_id)", email);
                return;
            } catch (Exception e1) {
                log.warn("execute-actions-email PUT without client_id failed: {}", e1.getMessage());
            }

            // Try PUT with client_id
            for (String cid : new String[]{"account", "account-console"}) {
                try {
                    identityClient.executeActionsEmail(adminInfo.adminPath, keycloakUserId,
                            authHeader, cid, redirectUri, actions);
                    log.info("Successfully triggered Keycloak verify email for {} using PUT (client_id={})", email, cid);
                    return;
                } catch (Exception e2) {
                    log.warn("execute-actions-email PUT with client_id={} failed: {}", cid, e2.getMessage());
                }
            }

            // Fallback to POST method
            try {
                identityClient.executeActionsEmailPost(adminInfo.adminPath, keycloakUserId,
                        authHeader, null, redirectUri, actions);
                log.info("Successfully triggered Keycloak verify email for {} using POST (no client_id)", email);
                return;
            } catch (Exception e3) {
                log.warn("execute-actions-email POST without client_id failed: {}", e3.getMessage());
            }

            for (String cid : new String[]{"account", "account-console"}) {
                try {
                    identityClient.executeActionsEmailPost(adminInfo.adminPath, keycloakUserId,
                            authHeader, cid, redirectUri, actions);
                    log.info("Successfully triggered Keycloak verify email for {} using POST (client_id={})", email, cid);
                    return;
                } catch (Exception e4) {
                    log.warn("execute-actions-email POST with client_id={} failed: {}", cid, e4.getMessage());
                }
            }

            log.warn("All attempts to send Keycloak verify email failed for {}. " +
                    "User was created with requiredActions=[VERIFY_EMAIL], so Keycloak may send email automatically when user first logs in.", email);
        } catch (Exception ex) {
            log.error("Failed to send Keycloak verify email for {}. Error: {}", email, ex.getMessage(), ex);
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public UserResponseDTO syncEmailVerification(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));

        try {
            KeycloakAdminInfo adminInfo = getKeycloakAdminInfo();
            if (adminInfo == null || adminInfo.token == null || adminInfo.adminPath == null) {
                throw new RuntimeException("Failed to get admin token from Keycloak");
            }

            var kcUser = identityClient.getUser(adminInfo.adminPath, userId,
                    "Bearer " + adminInfo.token.getAccessToken());
            
            Object emailVerifiedObj = kcUser.get("emailVerified");
            Boolean emailVerified = emailVerifiedObj instanceof Boolean ? (Boolean) emailVerifiedObj : 
                                   (emailVerifiedObj instanceof String && "true".equalsIgnoreCase(emailVerifiedObj.toString()));

            if (Boolean.TRUE.equals(emailVerified)) {
                log.info("Email verified in Keycloak for user: {}. Activating user.", userId);
                user.setStatus(User.UserStatus.ACTIVE);
                userRepository.save(user);
                
                // Ensure user is enabled in Keycloak
                Object enabledObj = kcUser.get("enabled");
                Boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : 
                                 (enabledObj instanceof String && "true".equalsIgnoreCase(enabledObj.toString()));
                if (!Boolean.TRUE.equals(enabled)) {
                    enableUserInKeycloak(userId);
                }
                
                return UserResponseDTO.fromEntity(user);
            } else {
                throw new RuntimeException("Email not verified in Keycloak for user: " + userId);
            }
        } catch (Exception e) {
            log.error("Failed to sync email verification for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to sync email verification: " + e.getMessage());
        }
    }

    private String extractUserId(ResponseEntity<?> response) {
        List<String> locations = response.getHeaders().get("Location");
        if(locations == null || locations.isEmpty()){
            throw new IllegalStateException("No location header found");
        }
        String location = locations.get(0);
        String[] split = location.split("/");
        return split[split.length - 1];
    }
}

