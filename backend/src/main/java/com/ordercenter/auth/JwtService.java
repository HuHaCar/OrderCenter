package com.ordercenter.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ordercenter.auth.entity.AppUser;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 只负责签发和校验 access token。
 * refresh token 是随机字符串，不经过这里。
 */
@Service
public class JwtService {

    private final AuthProperties properties;
    private final MACSigner signer;
    private final MACVerifier verifier;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        String secret = properties.getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.auth.jwt-secret 至少需要 32 字节");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        try {
            this.signer = new MACSigner(secretBytes);
            this.verifier = new MACVerifier(secretBytes);
        } catch (JOSEException exception) {
            throw new IllegalStateException("无法初始化 JWT 签名器", exception);
        }
    }

    public String issueAccessToken(AppUser user, Long sessionId) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience()))
                .subject(String.valueOf(user.getId()))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(properties.getAccessTokenTtl())))
                .claim("sid", sessionId)
                .claim("username", user.getUsername())
                .claim("displayName", user.getDisplayName())
                .claim("roles", List.of(user.getRole()))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("生成 access token 失败", exception);
        }
    }

    public AccessTokenClaims parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(verifier)) {
                throw new JwtTokenException("access token 签名无效");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiresAt = claims.getExpirationTime();
            if (expiresAt == null || expiresAt.before(new Date())) {
                throw new JwtTokenException("access token 已过期");
            }
            if (!properties.getIssuer().equals(claims.getIssuer())) {
                throw new JwtTokenException("access token issuer 不匹配");
            }
            if (claims.getAudience() == null
                    || !claims.getAudience().contains(properties.getAudience())) {
                throw new JwtTokenException("access token audience 不匹配");
            }

            Long userId = Long.valueOf(claims.getSubject());
            Long sessionId = claims.getLongClaim("sid");
            if (sessionId == null) {
                throw new JwtTokenException("access token 缺少会话信息");
            }

            List<String> roles = claims.getStringListClaim("roles");
            String role = roles == null || roles.isEmpty() ? "USER" : roles.getFirst();

            return new AccessTokenClaims(
                    userId,
                    claims.getStringClaim("username"),
                    claims.getStringClaim("displayName"),
                    role,
                    sessionId
            );
        } catch (ParseException | JOSEException | NumberFormatException exception) {
            throw new JwtTokenException("access token 无效", exception);
        }
    }

    public long accessExpiresInSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }

    /**
     * access token 中真正需要信任的只有用户 ID 和 session ID；
     * 展示名和角色仍以数据库中的当前值为准。
     */
    public record AccessTokenClaims(
            Long userId,
            String username,
            String displayName,
            String role,
            Long sessionId
    ) {
    }
}
