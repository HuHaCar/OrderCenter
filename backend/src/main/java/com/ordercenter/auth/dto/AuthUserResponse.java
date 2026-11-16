package com.ordercenter.auth.dto;

import com.ordercenter.auth.entity.AppUser;

public record AuthUserResponse(
        Long id,
        String username,
        String displayName,
        String role
) {

    public static AuthUserResponse from(AppUser user) {
        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole()
        );
    }
}
