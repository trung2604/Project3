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

        // If user is INACTIVE, check if email is verified in Keycloak
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            try {
                Map<String, Object> kcUser = keycloakService.getUser(user.getUserId());
                Boolean emailVerified = extractBooleanValue(kcUser, "emailVerified");
                
                if (Boolean.TRUE.equals(emailVerified)) {
                    log.info("Email verified in Keycloak for user: {}. Activating user.", request.getUsername());
                    user.setStatus(User.UserStatus.ACTIVE);
                    userRepository.save(user);
                    
                    // Enable user in Keycloak if not already enabled
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

        // Create user in local database
        User user = buildUserEntity(keycloakUserId, request);
        User savedUser = userRepository.save(user);
        
        // Send verification email
        keycloakService.sendVerificationEmail(savedUser.getUserId(), savedUser.getEmail());
        
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
            user.setEmail(request.getEmail());
            // Email change requires re-verify: set INACTIVE and trigger verify
            user.setStatus(User.UserStatus.INACTIVE);
            updateUserInKeycloakProfile(user.getUserId(), request.getFirstName(), request.getLastName(), user.getEmail(), false, true);
            keycloakService.sendVerificationEmail(user.getUserId(), user.getEmail());
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
                keycloakService.enableUser(user.getUserId());
            } else if (request.getStatus() != User.UserStatus.ACTIVE && oldStatus == User.UserStatus.ACTIVE) {
                keycloakService.disableUser(user.getUserId());
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
        return UserResponseDTO.fromEntity(updatedUser);
    }

    @Override
    public void deleteUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with userId: " + userId));
        
        // Delete user in Keycloak first
        try {
            keycloakService.deleteUser(user.getUserId());
        } catch (Exception e) {
            log.error("Failed to delete user in Keycloak: {}. Error: {}", user.getUserId(), e.getMessage());
            // Continue with local deletion even if Keycloak deletion fails
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
        user.setStatus(status);
        
        if (status == User.UserStatus.ACTIVE && oldStatus != User.UserStatus.ACTIVE) {
            keycloakService.enableUser(user.getUserId());
        } else if (status != User.UserStatus.ACTIVE && oldStatus == User.UserStatus.ACTIVE) {
            keycloakService.disableUser(user.getUserId());
        }
        
        User updatedUser = userRepository.save(user);
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
        return keycloakService.exchangeAuthorizationCode(req.getCode(), req.getRedirectUri());
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
                user.setStatus(User.UserStatus.ACTIVE);
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
}



