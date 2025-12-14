package com.project3.userservice.service;

import com.project3.userservice.constant.KeycloakConstants;
import com.project3.userservice.dto.*;
import com.project3.userservice.dto.identity.OAuthCodeExchangeRequest;
import com.project3.userservice.dto.identity.TokenExchangeResponse;
import com.project3.userservice.dto.identity.UserCreationRequest;
import com.project3.userservice.entity.User;
import com.project3.userservice.exception.*;
import com.project3.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final CloudinaryService cloudinaryService;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Attempting login for username: {}", request.getUsername());
        
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));
        
        log.info("User found in database. Status: {}", user.getStatus());
        
        // Authenticate with Keycloak
        TokenExchangeResponse tokenResponse = keycloakService.authenticateUser(
                request.getUsername(), request.getPassword());
        
        if (tokenResponse == null) {
            throw new AuthenticationException("Invalid username or password. Please verify your credentials or contact administrator if account is inactive.");
        }

        // Handle non-active statuses explicitly
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            if (user.getStatus() == User.UserStatus.BANNED) {
                log.warn("Banned user attempted login: {}", request.getUsername());
                throw new AuthenticationException("User account is banned. Please contact administrator.");
            }
            if (user.getStatus() == User.UserStatus.INACTIVE) {
                // For INACTIVE (typically awaiting email verification), check emailVerified in Keycloak
                try {
                    Map<String, Object> kcUser = keycloakService.getUser(user.getUserId());
                    Boolean emailVerified = extractBooleanValue(kcUser, "emailVerified");
                    if (Boolean.TRUE.equals(emailVerified)) {
                        log.info("Email verified in Keycloak for user: {}. Activating user.", request.getUsername());
                        user.activate();
                        userRepository.save(user);
                        // Ensure user is enabled in Keycloak
                        Boolean enabled = extractBooleanValue(kcUser, "enabled");
                        if (!Boolean.TRUE.equals(enabled)) {
                            keycloakService.enableUser(user.getUserId());
                        }
                    } else {
                        log.warn("Login attempt for inactive user with unverified email: {}. Status: {}", 
                                request.getUsername(), user.getStatus());
                        throw new AuthenticationException("User account is not active. Please verify your email before logging in.");
                    }
                } catch (Exception e) {
                    log.error("Failed to check email verification status: {}", e.getMessage());
                    throw new AuthenticationException("User account is not active. Please verify your email.");
                }
            } else {
                // Any other undefined non-active status
                log.warn("Login blocked for user {} due to unsupported status: {}", request.getUsername(), user.getStatus());
                throw new AuthenticationException("User account status does not allow login.");
            }
        }

        return buildLoginResponse(tokenResponse, user);
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
        // Validate email uniqueness
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Validate username uniqueness
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        // Build Keycloak user request
        UserCreationRequest keycloakUserRequest = buildKeycloakUserRequest(request);

        // Create user in Keycloak
        String keycloakUserId = keycloakService.createUser(keycloakUserRequest);
        log.info("User created in Keycloak with userId: {}", keycloakUserId);

        // Assign role to user in Keycloak (so it appears in JWT token)
        if (request.getRole() != null) {
            try {
                keycloakService.assignRoleToUser(keycloakUserId, request.getRole().name());
            } catch (Exception e) {
                log.warn("Failed to assign role to user in Keycloak, but user was created: {}", e.getMessage());
            }
        }

        // Create user in local database
        User user = buildUserEntity(keycloakUserId, request);
        User savedUser = userRepository.save(user);
        
        // Send verification email
        keycloakService.sendVerificationEmail(savedUser.getUserId(), savedUser.getEmail());
        
        // Publish user created event
        applicationEventPublisher.publishEvent(savedUser);
        
        return UserResponseDTO.fromEntity(savedUser);
    }

    @Override
    public UserResponseDTO updateUser(String userId, UpdateUserRequestDTO request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with userId: " + userId));

        boolean emailChanged = request.getEmail() != null && !request.getEmail().equals(user.getEmail());
        if (emailChanged) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new EmailAlreadyExistsException(request.getEmail());
            }
            // Use entity method for email change
            user.changeEmail(request.getEmail());
            updateUserInKeycloakProfile(user.getUserId(), request.getFirstName(), request.getLastName(), user.getEmail(), false, true);
            keycloakService.sendVerificationEmail(user.getUserId(), user.getEmail());
        }

        // Use entity method for profile update
        user.updateProfile(request.getFirstName(), request.getLastName(), request.getPhone(), request.getAddress());
        if (request.getRole() != null) user.setRole(request.getRole());
        
        User.UserStatus oldStatus = user.getStatus();
        if (request.getStatus() != null) {
            // Use entity methods for status changes
            if (request.getStatus() == User.UserStatus.ACTIVE && oldStatus != User.UserStatus.ACTIVE) {
                user.activate();
                keycloakService.enableUser(user.getUserId());
            } else if (request.getStatus() == User.UserStatus.INACTIVE && oldStatus == User.UserStatus.ACTIVE) {
                user.deactivate();
                keycloakService.disableUser(user.getUserId());
            } else if (request.getStatus() == User.UserStatus.BANNED) {
                user.ban();
                keycloakService.disableUser(user.getUserId());
            } else {
                user.setStatus(request.getStatus());
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

        // Sync profile fields to Keycloak if not handled above
        if (!emailChanged) {
            updateUserInKeycloakProfile(user.getUserId(), request.getFirstName(), request.getLastName(), null, null, false);
        }
        
        // Publish user updated event
        applicationEventPublisher.publishEvent(updatedUser);
        
        return UserResponseDTO.fromEntity(updatedUser);
    }

    @Override
    public void deleteUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with userId: " + userId));
        
        // Delete user in Keycloak first - MUST succeed before deleting from database
        try {
            keycloakService.deleteUser(user.getUserId());
            log.info("User {} deleted successfully from Keycloak", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to delete user in Keycloak: {}. Error: {}", user.getUserId(), e.getMessage());
            throw new KeycloakException("Cannot delete user: Failed to delete user from Keycloak. " + e.getMessage(), e);
        }

        // Delete avatar from Cloudinary (non-blocking, but log if fails)
        if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isEmpty()) {
            log.info("Deleting avatar with publicId: {} for user: {}", user.getAvatarPublicId(), userId);
            try {
                boolean deleted = cloudinaryService.deleteImage(user.getAvatarPublicId());
                if (!deleted) {
                    log.warn("Failed to delete avatar from Cloudinary for user: {}, but continuing with user deletion", userId);
                }
            } catch (Exception e) {
                log.warn("Error deleting avatar from Cloudinary for user {}: {}, but continuing with user deletion", userId, e.getMessage());
            }
        }
        
        // Delete user from database - only if Keycloak deletion succeeded
        userRepository.delete(user);
        log.info("User {} deleted successfully from database", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with userId: " + userId));
        return UserResponseDTO.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
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
                .orElseThrow(() -> new UserNotFoundException("User not found with userId: " + userId));
        
        User.UserStatus oldStatus = user.getStatus();
        
        // Use entity methods for status changes
        if (status == User.UserStatus.ACTIVE && oldStatus != User.UserStatus.ACTIVE) {
            user.activate();
            keycloakService.enableUser(user.getUserId());
        } else if (status == User.UserStatus.INACTIVE && oldStatus == User.UserStatus.ACTIVE) {
            user.deactivate();
            keycloakService.disableUser(user.getUserId());
        } else if (status == User.UserStatus.BANNED) {
            user.ban();
            keycloakService.disableUser(user.getUserId());
        } else {
            user.setStatus(status);
        }
        
        User updatedUser = userRepository.save(user);
        
        // Publish user updated event
        applicationEventPublisher.publishEvent(updatedUser);
        
        return UserResponseDTO.fromEntity(updatedUser);
    }

    @Override
    public UserResponseDTO updateUserAvatar(String userId, UpdateAvatarRequestDTO request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with userId: " + userId));

        // Delete old avatar if exists and different
        if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isEmpty()
                && !user.getAvatarPublicId().equals(request.getAvatarPublicId())) {
            log.info("Deleting old avatar with publicId: {} for user: {}", user.getAvatarPublicId(), userId);
            boolean deleted = cloudinaryService.deleteImage(user.getAvatarPublicId());
            if (!deleted) {
                log.warn("Failed to delete old avatar from Cloudinary, proceeding to update metadata");
            }
        }

        user.setAvatarUrl(request.getAvatarUrl());
        user.setAvatarPublicId(request.getAvatarPublicId());
        User updated = userRepository.save(user);
        
        // Publish user updated event
        applicationEventPublisher.publishEvent(updated);
        
        return UserResponseDTO.fromEntity(updated);
    }


    private void updateUserInKeycloakProfile(String keycloakUserId, String firstName, String lastName, String email,
                                             Boolean emailVerified, boolean ensureVerifyAction) {
        try {
            UserCreationRequest.UserCreationRequestBuilder builder = UserCreationRequest.builder();
            if (firstName != null) builder.firstName(firstName);
            if (lastName != null) builder.lastName(lastName);
            if (email != null) builder.email(email);
            if (emailVerified != null) builder.emailVerified(emailVerified);
            if (ensureVerifyAction) {
                builder.requiredActions(List.of(KeycloakConstants.VERIFY_EMAIL_ACTION));
            }

            keycloakService.updateUser(keycloakUserId, builder.build());
        } catch (Exception e) {
            log.error("Failed to sync profile to Keycloak for user {}: {}", keycloakUserId, e.getMessage());
        }
    }

    @Override
    public UserResponseDTO updateMyProfile(String userId, UpdateUserRequestDTO request) {
        // Reuse same rules as admin update; but enforce no status/role change here
        request.setRole(null);
        request.setStatus(null);
        return updateUser(userId, request);
    }

    @Override
    public void changeMyPassword(String userId, ChangePasswordRequestDTO request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with userId: " + userId));

        // Verify current password by attempting login
        TokenExchangeResponse login = keycloakService.authenticateUser(user.getUsername(), request.getCurrentPassword());
        if (login == null) {
            throw new AuthenticationException("Current password is incorrect");
        }

        // Reset password in Keycloak
        keycloakService.resetPassword(userId, request.getNewPassword());
        log.info("Password changed successfully for user {}", userId);
    }

    @Override
    public TokenExchangeResponse exchangeAuthorizationCode(OAuthCodeExchangeRequest req) {
        TokenExchangeResponse tokenResponse = keycloakService.exchangeAuthorizationCode(req.getCode(), req.getRedirectUri());
        
        // After successful token exchange, check and activate user if email is verified
        try {
            // Decode JWT token to get user ID (sub claim)
            String accessToken = tokenResponse.getAccessToken();
            String userId = extractUserIdFromToken(accessToken);
            
            if (userId != null) {
                // Find user in database
                userRepository.findByUserId(userId).ifPresent(user -> {
                    // If user is INACTIVE, check if email is verified in Keycloak
                    if (user.getStatus() == User.UserStatus.INACTIVE) {
                        try {
                            Map<String, Object> kcUser = keycloakService.getUser(userId);
                            Boolean emailVerified = extractBooleanValue(kcUser, "emailVerified");
                            
                            if (Boolean.TRUE.equals(emailVerified)) {
                                log.info("Email verified in Keycloak for user: {}. Activating user during token exchange.", userId);
                                user.activate();
                                userRepository.save(user);
                                
                                // Ensure user is enabled in Keycloak
                                Boolean enabled = extractBooleanValue(kcUser, "enabled");
                                if (!Boolean.TRUE.equals(enabled)) {
                                    keycloakService.enableUser(userId);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to check email verification status during token exchange for user {}: {}", userId, e.getMessage());
                        }
                    } else if (user.getStatus() == User.UserStatus.BANNED) {
                        log.warn("Banned user attempted login via OAuth: {}", userId);
                        throw new AuthenticationException("User account is banned. Please contact administrator.");
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to process user activation during token exchange: {}", e.getMessage());
            // Don't fail the token exchange if activation check fails
        }
        
        return tokenResponse;
    }
    
    /**
     * Extract user ID from JWT token (sub claim)
     */
    private String extractUserIdFromToken(String token) {
        try {
            // JWT token format: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT token format");
                return null;
            }
            
            // Decode payload (base64)
            String payload = parts[1];
            // Add padding if needed
            int padding = 4 - (payload.length() % 4);
            if (padding != 4) {
                payload = payload + "=".repeat(padding);
            }
            
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            
            // Parse JSON to get "sub" claim
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(decodedPayload);
            return jsonNode.has("sub") ? jsonNode.get("sub").asText() : null;
        } catch (Exception e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    private UserCreationRequest buildKeycloakUserRequest(CreateUserRequestDTO request) {
        UserCreationRequest keycloakUserRequest = UserCreationRequest.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .enabled(true)
                .emailVerified(false)
                .requiredActions(List.of(KeycloakConstants.VERIFY_EMAIL_ACTION))
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

    // Helper methods
    
    private Boolean extractBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
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
    
    @Override
    public UserResponseDTO syncEmailVerification(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with userId: " + userId));

        try {
            Map<String, Object> kcUser = keycloakService.getUser(userId);
            Boolean emailVerified = extractBooleanValue(kcUser, "emailVerified");

            if (Boolean.TRUE.equals(emailVerified)) {
                log.info("Email verified in Keycloak for user: {}. Activating user.", userId);
                user.activate();
                userRepository.save(user);
                
                // Ensure user is enabled in Keycloak
                Boolean enabled = extractBooleanValue(kcUser, "enabled");
                if (!Boolean.TRUE.equals(enabled)) {
                    keycloakService.enableUser(userId);
                }
                
                return UserResponseDTO.fromEntity(user);
            } else {
                throw new KeycloakException("Email not verified in Keycloak for user: " + userId);
            }
        } catch (KeycloakException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to sync email verification for user {}: {}", userId, e.getMessage());
            throw new KeycloakException("Failed to sync email verification: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void initializeKeycloakRoles() {
        log.info("Initializing Keycloak roles via UserService");
        keycloakService.initializeRoles();
    }
    
    @Override
    public List<Map<String, Object>> getRealmRoles() {
        return keycloakService.getRealmRoles();
    }
    
    @Override
    public List<Map<String, Object>> getClientRoles() {
        return keycloakService.getClientRoles();
    }
    
    @Override
    @Transactional
    public void syncDeletedUsersFromKeycloak() {
        log.info("Starting sync: checking for users deleted in Keycloak");
        try {
            List<User> allUsers = userRepository.findAll();
            int deletedCount = 0;
            
            for (User user : allUsers) {
                if (!keycloakService.userExistsInKeycloak(user.getUserId())) {
                    log.info("User {} (email: {}) was deleted in Keycloak, deleting from database", 
                            user.getUserId(), user.getEmail());
                    
                    // Delete avatar from Cloudinary (non-blocking)
                    if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isEmpty()) {
                        try {
                            cloudinaryService.deleteImage(user.getAvatarPublicId());
                        } catch (Exception e) {
                            log.warn("Failed to delete avatar from Cloudinary for user {}: {}", 
                                    user.getUserId(), e.getMessage());
                        }
                    }
                    
                    // Delete from database
                    userRepository.delete(user);
                    deletedCount++;
                    log.info("User {} deleted from database", user.getUserId());
                }
            }
            
            if (deletedCount > 0) {
                log.info("Sync completed: {} users deleted from database", deletedCount);
            } else {
                log.debug("Sync completed: no users to delete");
            }
        } catch (Exception e) {
            log.error("Error during sync deleted users from Keycloak: {}", e.getMessage(), e);
        }
    }
}



