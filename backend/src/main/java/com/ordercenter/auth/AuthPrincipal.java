package com.ordercenter.auth;

import java.security.Principal;

/**
 * 放入 Spring Security SecurityContext 的当前登录人信息。
 */
public record AuthPrincipal(
        Long userId,
        String username,
        String displayName,
        String role,
        Long sessionId
) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
