CREATE TABLE product_variant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    variant_code VARCHAR(100) NOT NULL,
    name_zh VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_variant_product FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_product_variant_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    UNIQUE KEY uk_product_variant_product_code (product_id, variant_code),
    KEY idx_product_variant_product_sort (product_id, sort_order, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE product_variant_value (
    id BIGINT NOT NULL AUTO_INCREMENT,
    variant_id BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    option_id BIGINT NULL,
    value_zh TEXT NULL,
    value_en TEXT NULL,
    numeric_value DECIMAL(20, 6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_variant_value_variant FOREIGN KEY (variant_id) REFERENCES product_variant (id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    UNIQUE KEY uk_product_variant_value_attribute (variant_id, attribute_id),
    KEY idx_product_variant_value_attribute (attribute_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE product_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    media_id BIGINT NULL,
    file_name VARCHAR(255) NOT NULL,
    display_name_zh VARCHAR(200) NULL,
    display_name_en VARCHAR(200) NULL,
    file_url VARCHAR(1024) NOT NULL,
    file_type VARCHAR(127) NOT NULL,
    is_public TINYINT NOT NULL DEFAULT 0,
    allow_download TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_attachment_product FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_product_attachment_media FOREIGN KEY (media_id) REFERENCES media_file (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_product_attachment_public CHECK (is_public IN (0, 1)),
    CONSTRAINT chk_product_attachment_download CHECK (allow_download IN (0, 1)),
    KEY idx_product_attachment_product_sort (product_id, sort_order, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
