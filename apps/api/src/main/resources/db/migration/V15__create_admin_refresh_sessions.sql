CREATE TABLE admin_refresh_session (
    session_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    admin_user_id BIGINT NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    token_version INT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    replacement_session_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (session_id),
    UNIQUE KEY uk_admin_refresh_session_token_hash (token_hash),
    KEY idx_admin_refresh_session_admin_expiry (admin_user_id, expires_at),
    CONSTRAINT fk_admin_refresh_session_admin_user
        FOREIGN KEY (admin_user_id) REFERENCES admin_user (id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
