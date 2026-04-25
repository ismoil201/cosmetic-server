package com.example.backend.global.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static ApiResponse<?> okMessage(String msg) {
        return new ApiResponse<>(true, msg, null);
    }

    public static ApiResponse<?> error(String msg) {
        return new ApiResponse<>(false, msg, null);
    }
}
