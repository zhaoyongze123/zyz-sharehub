package com.sharehub.common;

public record ApiResponse<T>(boolean success, String code, T data, String message) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, "OK");
    }

    public static <T> ApiResponse<T> fail(String code) {
        return new ApiResponse<>(false, code, null, code);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, null, message);
    }
}
