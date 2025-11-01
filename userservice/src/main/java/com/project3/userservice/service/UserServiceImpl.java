package com.project3.userservice.service;

import com.project3.userservice.dto.CreateUserRequestDTO;
import com.project3.userservice.dto.LoginRequestDTO;
import com.project3.userservice.dto.LoginResponseDTO;
import com.project3.userservice.dto.PagedUserResponseDTO;
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
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Login attempt for inactive user: {}. Status: {}", request.getUsername(), user.getStatus());
            throw new RuntimeException("User account is not active. Current status: " + user.getStatus());
        }
        
        TokenExchangeResponse tokenResponse = authenticateUserWithKeycloak(
                request.getUsername(), request.getPassword());
        
        if (tokenResponse == null) {
            throw new RuntimeException("Invalid username or password. Please verify your credentials or contact administrator if account is inactive.");
        }

        LoginResponseDTO response = buildLoginResponse(tokenResponse, user);
        return response;
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
                .enabled(false)
                .emailVerified(false)
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

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
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

