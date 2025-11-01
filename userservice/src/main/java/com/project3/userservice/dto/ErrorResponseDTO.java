package com.project3.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDTO {
    private int status;
    private String message;
    private String error;
    private LocalDateTime timestamp;
    private String path;

    public ErrorResponseDTO(int status, String message, String error, String path) {
        this.status = status;
        this.message = message;
        this.error = error;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponseDTO badRequest(String message, String path) {
        return new ErrorResponseDTO(400, message, "Bad Request", path);
    }

    public static ErrorResponseDTO notFound(String message, String path) {
        return new ErrorResponseDTO(404, message, "Not Found", path);
    }

    public static ErrorResponseDTO conflict(String message, String path) {
        return new ErrorResponseDTO(409, message, "Conflict", path);
    }

    public static ErrorResponseDTO internalServerError(String message, String path) {
        return new ErrorResponseDTO(500, message, "Internal Server Error", path);
    }
}

