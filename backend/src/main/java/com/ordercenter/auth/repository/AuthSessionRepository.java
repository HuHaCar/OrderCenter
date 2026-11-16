package com.ordercenter.auth.repository;

import com.ordercenter.auth.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
}
