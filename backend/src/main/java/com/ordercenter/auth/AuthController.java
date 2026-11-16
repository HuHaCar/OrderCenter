package com.ordercenter.auth;

import com.ordercenter.auth.dto.AuthTokenResponse;
import com.ordercenter.auth.dto.AuthUserResponse;
import com.ordercenter.auth.dto.LoginRequest;
import com.ordercenter.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "登录认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE = "Lax";

    private final AuthService authService;
    private final AuthProperties properties;

    public AuthController(AuthService authService, AuthProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResult result = authService.login(
                request,
                httpRequest.getHeader(HttpHeaders.USER_AGENT),
                httpRequest.getRemoteAddr()
        );
        return authResponse(result);
    }

    @Operation(summary = "刷新 access token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest httpRequest
    ) {
        AuthResult result = authService.refresh(
                refreshToken,
                httpRequest.getHeader(HttpHeaders.USER_AGENT),
                httpRequest.getRemoteAddr()
        );
        return authResponse(result);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            Authentication authentication
    ) {
        Long sessionId = null;
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            sessionId = principal.sessionId();
        }

        authService.logout(sessionId, refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponse.ok(null));
    }

    @Operation(summary = "当前登录人")
    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BadCredentialsException("登录已失效");
        }

        return ApiResponse.ok(new AuthUserResponse(
                principal.userId(),
                principal.username(),
                principal.displayName(),
                principal.role()
        ));
    }

    private ResponseEntity<ApiResponse<AuthTokenResponse>> authResponse(AuthResult result) {
        AuthTokenResponse body = new AuthTokenResponse(
                result.accessToken(),
                result.accessExpiresInSeconds(),
                result.user()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken(), result.refreshExpiresInSeconds())
                                .toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponse.ok(body));
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
