-- 登录用户
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    display_name  VARCHAR(50)  NOT NULL COMMENT '展示名称',
    role          VARCHAR(20)  NOT NULL COMMENT '角色：ADMIN / OPERATOR',
    enabled       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at    DATETIME(6)  NOT NULL COMMENT '创建时间',
    updated_at    DATETIME(6)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '登录用户表';

-- 一次登录对应一个会话；access token 中的 sid 指向这里。
CREATE TABLE IF NOT EXISTS auth_sessions (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话主键 / JWT sid',
    user_id      BIGINT       NOT NULL COMMENT '用户 ID',
    expires_at   DATETIME(6)  NOT NULL COMMENT '会话过期时间',
    revoked_at   DATETIME(6)  NULL COMMENT '撤销时间，退出登录后立即失效',
    created_at   DATETIME(6)  NOT NULL COMMENT '创建时间',
    last_seen_at DATETIME(6)  NULL COMMENT '最近刷新时间',
    user_agent   VARCHAR(255) NULL COMMENT '登录设备 UA',
    ip_address   VARCHAR(45)  NULL COMMENT '登录 IP',
    PRIMARY KEY (id),
    KEY idx_auth_sessions_user (user_id),
    KEY idx_auth_sessions_expires (expires_at),
    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '登录会话表';

-- refresh token 只保存哈希；原始 token 仅存在于 HttpOnly Cookie。
CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id       BIGINT      NOT NULL COMMENT '所属会话',
    token_hash       CHAR(64)    NOT NULL COMMENT 'SHA-256 哈希',
    expires_at       DATETIME(6) NOT NULL COMMENT '过期时间',
    revoked_at       DATETIME(6) NULL COMMENT '撤销时间',
    replaced_by_hash CHAR(64)    NULL COMMENT '轮换后的新 token 哈希',
    created_at       DATETIME(6) NOT NULL COMMENT '创建时间',
    last_used_at     DATETIME(6) NULL COMMENT '最近使用时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_refresh_token_hash (token_hash),
    KEY idx_auth_refresh_session (session_id),
    KEY idx_auth_refresh_expires (expires_at),
    CONSTRAINT fk_auth_refresh_session
        FOREIGN KEY (session_id) REFERENCES auth_sessions (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '刷新令牌表';
