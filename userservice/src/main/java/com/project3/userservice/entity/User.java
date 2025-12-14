package com.project3.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.CUSTOMER;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.INACTIVE;  // INACTIVE by default until email verification

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "avatar_public_id")
    private String avatarPublicId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (userId == null || userId.isEmpty()) {
            userId = "user-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UserRole {
        CUSTOMER,           // Khách hàng
        STAFF,              // Nhân viên phục vụ
        WAREHOUSE_STAFF,    // Nhân viên kho
        RESTAURANT_MANAGER, // Quản lý nhà hàng
        ADMIN               // Quản trị viên hệ thống
    }

    public enum UserStatus {
        ACTIVE,      // Hoạt động
        INACTIVE,    // Tạm dừng
        BANNED       // Bị cấm
    }
    
    /**
     * Business logic: Checks if user is active
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
    
    /**
     * Business logic: Activates the user
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }
    
    /**
     * Business logic: Deactivates the user
     */
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }
    
    /**
     * Business logic: Bans the user
     */
    public void ban() {
        this.status = UserStatus.BANNED;
    }
    
    /**
     * Business logic: Updates user profile information
     */
    public void updateProfile(String firstName, String lastName, String phone, String address) {
        if (firstName != null) this.firstName = firstName;
        if (lastName != null) this.lastName = lastName;
        if (phone != null) this.phone = phone;
        if (address != null) this.address = address;
    }
    
    /**
     * Business logic: Updates email and sets status to INACTIVE (requires re-verification)
     */
    public void changeEmail(String newEmail) {
        if (newEmail == null || newEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        this.email = newEmail;
        // Email change requires re-verification
        this.status = UserStatus.INACTIVE;
    }
    
    /**
     * Business logic: Checks if user has a specific role
     */
    public boolean hasRole(UserRole role) {
        return this.role == role;
    }
    
    /**
     * Business logic: Checks if user is staff or above
     */
    public boolean isStaffOrAbove() {
        return this.role == UserRole.STAFF 
            || this.role == UserRole.WAREHOUSE_STAFF
            || this.role == UserRole.RESTAURANT_MANAGER
            || this.role == UserRole.ADMIN;
    }
    
    /**
     * Business logic: Checks if user is admin or manager
     */
    public boolean isAdminOrManager() {
        return this.role == UserRole.ADMIN || this.role == UserRole.RESTAURANT_MANAGER;
    }
}
