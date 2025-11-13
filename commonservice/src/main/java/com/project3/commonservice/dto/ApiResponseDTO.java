package com.project3.commonservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponseDTO<T> {
    private Integer statusCode;
    private String message;
    private T data;

    public static <T> ApiResponseDTO<T> success(T data, String message) {
        ApiResponseDTO<T> response = new ApiResponseDTO<>();
        response.setStatusCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    public static <T> ApiResponseDTO<T> created(T data, String message) {
        ApiResponseDTO<T> response = new ApiResponseDTO<>();
        response.setStatusCode(201);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    public static <T> ApiResponseDTO<T> noContent(String message) {
        ApiResponseDTO<T> response = new ApiResponseDTO<>();
        response.setStatusCode(204);
        response.setMessage(message);
        response.setData(null);
        return response;
    }

    public static <T> ApiResponseDTO<T> error(String message, Integer statusCode) {
        ApiResponseDTO<T> response = new ApiResponseDTO<>();
        response.setStatusCode(statusCode);
        response.setMessage(message);
        response.setData(null);
        return response;
    }

    public static <T> ApiResponseDTO<T> badRequest(String message) {
        return error(message, 400);
    }

    public static <T> ApiResponseDTO<T> notFound(String message) {
        return error(message, 404);
    }

    public static <T> ApiResponseDTO<T> internalServerError(String message) {
        return error(message, 500);
    }
}

