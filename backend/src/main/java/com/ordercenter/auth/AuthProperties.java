package com.ordercenter.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 登录相关配置。
 * 生产环境务必通过环境变量覆盖 JWT_SECRET 和 AUTH_COOKIE_SECURE。
 */
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String jwtSecret = "order-center-dev-secret-change-me-2026";
    private Duration accessTokenTtl = Duration.ofMinutes(5);
    private Duration refreshTokenTtl = Duration.ofDays(2);
    private boolean cookieSecure = false;

    private String defaultUsername = "admin";
    private String defaultPassword = "admin123";
    private String defaultDisplayName = "系统管理员";
    private String defaultRole = "ADMIN";

    private String issuer = "order-center";
    private String audience = "order-center-web";

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getDefaultUsername() {
        return defaultUsername;
    }

    public void setDefaultUsername(String defaultUsername) {
        this.defaultUsername = defaultUsername;
    }

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    public String getDefaultDisplayName() {
        return defaultDisplayName;
    }

    public void setDefaultDisplayName(String defaultDisplayName) {
        this.defaultDisplayName = defaultDisplayName;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}
