CREATE TABLE admin_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    token_version INT NOT NULL DEFAULT 0,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE admin_login_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_user_id BIGINT NULL,
    username VARCHAR(100) NOT NULL,
    login_success TINYINT NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(512) NULL,
    failure_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_admin_login_log_admin_user
        FOREIGN KEY (admin_user_id) REFERENCES admin_user (id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    KEY idx_admin_login_log_username_created_at (username, created_at),
    KEY idx_admin_login_log_ip_address_created_at (ip_address, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE admin_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_user_id BIGINT NULL,
    operation VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id BIGINT NULL,
    detail JSON NULL,
    ip_address VARCHAR(45) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_admin_operation_log_admin_user
        FOREIGN KEY (admin_user_id) REFERENCES admin_user (id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    KEY idx_admin_operation_log_admin_user_id_created_at (admin_user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
