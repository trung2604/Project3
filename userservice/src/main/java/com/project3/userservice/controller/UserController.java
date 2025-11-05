package com.project3.userservice.controller;

import com.project3.userservice.dto.*;
import com.project3.userservice.entity.User;
import com.project3.userservice.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private IUserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            LoginResponseDTO response = userService.login(request);
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Login successful"));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Login failed";
            log.error("Login failed: {}", errorMessage);
            
            int statusCode = errorMessage.contains("not active") ? 403 : 401;
            ErrorResponseDTO error = new ErrorResponseDTO(
                    statusCode,
                    errorMessage,
                    statusCode == 403 ? "Forbidden" : "Unauthorized",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(statusCode == 403 ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> register(
            @Valid @RequestBody RegisterUserRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            UserResponseDTO user = userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.created(user, "User registered successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.badRequest(
                    e.getMessage() != null ? e.getMessage() : "Failed to register user",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createUser(
            @Valid @RequestBody CreateUserRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            UserResponseDTO user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.created(user, "User created successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.badRequest(
                    e.getMessage() != null ? e.getMessage() : "Failed to create user",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }


    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            UserResponseDTO user = userService.updateUser(userId, request);
            return ResponseEntity.ok(ApiResponseDTO.success(user, "User updated successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.notFound(
                    e.getMessage() != null ? e.getMessage() : "User not found with userId: " + userId,
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteUser(
            @PathVariable String userId,
            HttpServletRequest httpRequest) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponseDTO.noContent("User deleted successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.notFound(
                    e.getMessage() != null ? e.getMessage() : "User not found with userId: " + userId,
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserById(
            @PathVariable String userId,
            HttpServletRequest httpRequest) {
        try {
            UserResponseDTO user = userService.getUserById(userId);
            return ResponseEntity.ok(ApiResponseDTO.success(user, "User retrieved successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.notFound(
                    e.getMessage() != null ? e.getMessage() : "User not found with userId: " + userId,
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserByEmail(
            @PathVariable String email,
            HttpServletRequest httpRequest) {
        try {
            UserResponseDTO user = userService.getUserByEmail(email);
            return ResponseEntity.ok(ApiResponseDTO.success(user, "User retrieved successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.notFound(
                    e.getMessage() != null ? e.getMessage() : "User not found with email: " + email,
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedUserResponseDTO>> getAllUsers(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) User.UserRole role,
            @RequestParam(required = false) User.UserStatus status) {
        try {
            PagedUserResponseDTO response = userService.getAllUsers(page, size, search, role, status);
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Users retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to retrieve users: " + e.getMessage(), null, 500));
        }
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> toggleUserStatus(
            @PathVariable String userId,
            @RequestParam User.UserStatus status,
            HttpServletRequest httpRequest) {
        try {
            UserResponseDTO user = userService.toggleUserStatus(userId, status);
            return ResponseEntity.ok(ApiResponseDTO.success(user, "User status updated successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.notFound(
                    e.getMessage() != null ? e.getMessage() : "User not found with userId: " + userId,
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @PostMapping("/{userId}/verify-email")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> verifyEmail(
            @PathVariable String userId,
            HttpServletRequest httpRequest) {
        try {
            UserResponseDTO user = userService.syncEmailVerification(userId);
            return ResponseEntity.ok(ApiResponseDTO.success(user, "Email verified successfully. Account activated."));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.badRequest(
                    e.getMessage() != null ? e.getMessage() : "Failed to verify email",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @PostMapping("/oauth/token-exchange")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> exchangeCode(
            @RequestBody com.project3.userservice.dto.identity.OAuthCodeExchangeRequest request,
            HttpServletRequest httpRequest) {
        try {
            var token = userService.exchangeAuthorizationCode(request);
            LoginResponseDTO response = new LoginResponseDTO();
            response.setAccessToken(token.getAccessToken());
            response.setRefreshToken(token.getRefreshToken() != null ? token.getRefreshToken() : token.getIdToken());
            response.setTokenType(token.getTokenType());
            response.setExpiresIn(Long.parseLong(token.getExpiresIn()));
            response.setRefreshExpiresIn(token.getRefreshExpiresIn() != null ? Long.parseLong(token.getRefreshExpiresIn()) : 1800L);
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Token exchanged successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.badRequest(
                    e.getMessage() != null ? e.getMessage() : "Failed to exchange code",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            HttpServletRequest httpRequest) {
        try {
            String userId = userIdHeader;
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponseDTO<>(false, "Missing X-User-Id header", null, 401));
            }
            UserResponseDTO user = userService.getUserById(userId);
            return ResponseEntity.ok(ApiResponseDTO.success(user, "User retrieved successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.notFound(
                    e.getMessage() != null ? e.getMessage() : "User not found",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateMyProfile(
            @Valid @RequestBody UpdateUserRequestDTO request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            String userId = userIdHeader;
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponseDTO<>(false, "Missing X-User-Id header", null, 401));
            }
            UserResponseDTO user = userService.updateMyProfile(userId, request);
            return ResponseEntity.ok(ApiResponseDTO.success(user, "Profile updated successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.badRequest(
                    e.getMessage() != null ? e.getMessage() : "Failed to update profile",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponseDTO<Void>> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            String userId = userIdHeader;
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponseDTO<>(false, "Missing X-User-Id header", null, 401));
            }
            userService.changeMyPassword(userId, request);
            return ResponseEntity.ok(ApiResponseDTO.success(null, "Password changed successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.badRequest(
                    e.getMessage() != null ? e.getMessage() : "Failed to change password",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }
    @PatchMapping("/{userId}/avatar")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateAvatar(
            @PathVariable String userId,
            @Valid @RequestBody UpdateAvatarRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            UserResponseDTO user = userService.updateUserAvatar(userId, request);
            return ResponseEntity.ok(ApiResponseDTO.success(user, "Avatar updated successfully"));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = ErrorResponseDTO.badRequest(
                    e.getMessage() != null ? e.getMessage() : "Failed to update avatar",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, error.getMessage(), null, error.getStatus()));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest httpRequest) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseDTO<>(false, "Validation failed", errors, 400));
    }
}
