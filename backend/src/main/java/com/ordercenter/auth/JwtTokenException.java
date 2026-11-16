package com.ordercenter.auth;

/**
 * access token 无法验证时使用；过滤器会将其视为未登录。
 */
public class JwtTokenException extends RuntimeException {

    public JwtTokenException(String message) {
        super(message);
    }

    public JwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
