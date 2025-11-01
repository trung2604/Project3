package com.project3.userservice.dto;

import com.project3.userservice.entity.User;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequestDTO {
    @Email(message = "Email must be valid")
    private String email;

    private String firstName;

    private String lastName;

    private String phone;

    private String address;

    private User.UserRole role;

    private User.UserStatus status;

    private String avatarUrl;

    private String avatarPublicId;

    private LocalDate dateOfBirth;
}

