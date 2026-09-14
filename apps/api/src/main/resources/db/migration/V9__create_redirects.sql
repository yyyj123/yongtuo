CREATE TABLE url_redirect (
    id BIGINT NOT NULL AUTO_INCREMENT,
    old_path VARCHAR(512) NOT NULL,
    new_path VARCHAR(1024) NOT NULL,
    redirect_type SMALLINT NOT NULL DEFAULT 301,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_url_redirect_type CHECK (redirect_type = 301),
    UNIQUE KEY uk_url_redirect_old_path (old_path)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
