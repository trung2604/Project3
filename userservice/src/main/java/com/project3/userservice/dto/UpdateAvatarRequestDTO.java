package com.project3.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAvatarRequestDTO {
    @NotBlank(message = "avatarUrl is required")
    private String avatarUrl;

    @NotBlank(message = "avatarPublicId is required")
    private String avatarPublicId;
}


