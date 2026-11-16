package com.ordercenter.auth;

/**
 * refresh token 缺失、过期、已撤销或被重放时使用，统一返回 401。
 */
public class RefreshTokenException extends RuntimeException {

    public RefreshTokenException(String message) {
        super(message);
    }
}
