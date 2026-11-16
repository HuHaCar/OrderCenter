package com.ordercenter.auth.repository;

import com.ordercenter.auth.entity.AuthRefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

    /**
     * 刷新时锁住 token 行，避免同一个 refresh token 被并发消费两次。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthRefreshToken t where t.tokenHash = :tokenHash")
    Optional<AuthRefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            update AuthRefreshToken t
               set t.revokedAt = :revokedAt
             where t.sessionId = :sessionId
               and t.revokedAt is null
            """)
    int revokeAllBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}
