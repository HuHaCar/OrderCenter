package com.ordercenter.auth;

import com.ordercenter.auth.dto.AuthUserResponse;
import com.ordercenter.auth.dto.LoginRequest;
import com.ordercenter.auth.entity.AppUser;
import com.ordercenter.auth.entity.AuthRefreshToken;
import com.ordercenter.auth.entity.AuthSession;
import com.ordercenter.auth.repository.AppUserRepository;
import com.ordercenter.auth.repository.AuthRefreshTokenRepository;
import com.ordercenter.auth.repository.AuthSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_USER_AGENT_LENGTH = 255;
    private static final int MAX_IP_LENGTH = 45;

    private final AppUserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;

    public AuthService(
            AppUserRepository userRepository,
            AuthSessionRepository sessionRepository,
            AuthRefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthProperties properties
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional
    public AuthResult login(
            LoginRequest request,
            String userAgent,
            String ipAddress
    ) {
        String username = request.username().trim();
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        if (!user.isEnabled()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(properties.getRefreshTokenTtl());

        AuthSession session = new AuthSession();
        session.setUserId(user.getId());
        session.setExpiresAt(expiresAt);
        session.setLastSeenAt(now);
        session.setUserAgent(truncate(userAgent, MAX_USER_AGENT_LENGTH));
        session.setIpAddress(truncate(ipAddress, MAX_IP_LENGTH));
        session = sessionRepository.save(session);

        String rawRefreshToken = issueRefreshToken(session.getId(), expiresAt);
        String accessToken = jwtService.issueAccessToken(user, session.getId());

        return new AuthResult(
                accessToken,
                jwtService.accessExpiresInSeconds(),
                rawRefreshToken,
                properties.getRefreshTokenTtl().toSeconds(),
                AuthUserResponse.from(user)
        );
    }

    /**
     * 刷新时轮换 refresh token。
     * 旧 token 被重放时撤销整个会话；noRollbackFor 保证撤销动作真正落库。
     */
    @Transactional(noRollbackFor = RefreshTokenException.class)
    public AuthResult refresh(
            String rawRefreshToken,
            String userAgent,
            String ipAddress
    ) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new RefreshTokenException("登录已失效，请重新登录");
        }

        LocalDateTime now = LocalDateTime.now();
        String tokenHash = TokenUtils.sha256(rawRefreshToken);
        AuthRefreshToken current = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new RefreshTokenException("登录已失效，请重新登录"));

        if (current.getRevokedAt() != null) {
            log.warn("检测到 refresh token 重放，撤销会话 {}", current.getSessionId());
            revokeSession(current.getSessionId(), now);
            throw new RefreshTokenException("登录状态已失效，请重新登录");
        }

        if (!current.getExpiresAt().isAfter(now)) {
            throw new RefreshTokenException("登录已过期，请重新登录");
        }

        AuthSession session = sessionRepository.findById(current.getSessionId())
                .orElseThrow(() -> new RefreshTokenException("登录会话不存在"));
        if (!session.isActive(now)) {
            throw new RefreshTokenException("登录状态已失效，请重新登录");
        }

        AppUser user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new RefreshTokenException("用户不存在"));
        if (!user.isEnabled()) {
            revokeSession(session.getId(), now);
            throw new RefreshTokenException("账号已停用");
        }

        String newRawRefreshToken = TokenUtils.newRefreshToken();
        String newTokenHash = TokenUtils.sha256(newRawRefreshToken);
        LocalDateTime nextExpiresAt = now.plus(properties.getRefreshTokenTtl());

        current.setRevokedAt(now);
        current.setLastUsedAt(now);
        current.setReplacedByHash(newTokenHash);
        refreshTokenRepository.save(current);

        AuthRefreshToken next = new AuthRefreshToken();
        next.setSessionId(session.getId());
        next.setTokenHash(newTokenHash);
        next.setExpiresAt(nextExpiresAt);
        refreshTokenRepository.save(next);

        session.setExpiresAt(nextExpiresAt);
        session.setLastSeenAt(now);
        session.setUserAgent(truncate(userAgent, MAX_USER_AGENT_LENGTH));
        session.setIpAddress(truncate(ipAddress, MAX_IP_LENGTH));
        sessionRepository.save(session);

        String accessToken = jwtService.issueAccessToken(user, session.getId());

        return new AuthResult(
                accessToken,
                jwtService.accessExpiresInSeconds(),
                newRawRefreshToken,
                properties.getRefreshTokenTtl().toSeconds(),
                AuthUserResponse.from(user)
        );
    }

    /**
     * 退出登录：优先按 access token 的 sid 撤销；
     * 如果 access 已过期，则用 refresh cookie 找到会话并撤销。
     */
    @Transactional
    public void logout(Long sessionId, String rawRefreshToken) {
        LocalDateTime now = LocalDateTime.now();
        Set<Long> sessionIds = new LinkedHashSet<>();

        if (sessionId != null) {
            sessionIds.add(sessionId);
        }
        if (StringUtils.hasText(rawRefreshToken)) {
            refreshTokenRepository.findByTokenHash(TokenUtils.sha256(rawRefreshToken))
                    .ifPresent(token -> sessionIds.add(token.getSessionId()));
        }

        sessionIds.forEach(id -> revokeSession(id, now));
    }

    private String issueRefreshToken(Long sessionId, LocalDateTime expiresAt) {
        String rawToken = TokenUtils.newRefreshToken();
        AuthRefreshToken token = new AuthRefreshToken();
        token.setSessionId(sessionId);
        token.setTokenHash(TokenUtils.sha256(rawToken));
        token.setExpiresAt(expiresAt);
        refreshTokenRepository.save(token);
        return rawToken;
    }

    private void revokeSession(Long sessionId, LocalDateTime now) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(now);
                sessionRepository.save(session);
            }
        });
        refreshTokenRepository.revokeAllBySessionId(sessionId, now);
    }

    private static String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
