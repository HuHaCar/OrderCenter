package com.ordercenter.auth.dto;

/**
 * 登录 / 刷新成功后返回给前端的 access token 信息。
 * refresh token 不放在响应体里，只放在 HttpOnly Cookie。
 */
public record AuthTokenResponse(
        String accessToken,
        long expiresIn,
        AuthUserResponse user
) {
}
