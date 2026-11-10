package com.ordercenter.common;

/**
 * 统一 API 响应结构。
 * 前端 Axios 拦截器只处理 code === 0 的数据，其余统一抛错。
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
