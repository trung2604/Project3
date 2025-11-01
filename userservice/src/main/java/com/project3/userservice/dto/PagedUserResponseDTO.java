package com.project3.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedUserResponseDTO {
    private List<UserResponseDTO> users;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}

