package com.ordercenter.auth;

import com.ordercenter.auth.dto.AuthUserResponse;

/**
 * Service 层内部结果：包含需要写入 Cookie 的原始 refresh token。
 */
public record AuthResult(
        String accessToken,
        long accessExpiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds,
        AuthUserResponse user
) {
}
