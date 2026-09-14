CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    product_code VARCHAR(100) NOT NULL,
    name_zh VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NULL,
    slug VARCHAR(191) NOT NULL,
    summary_zh TEXT NULL,
    summary_en TEXT NULL,
    description_zh LONGTEXT NULL,
    description_en LONGTEXT NULL,
    english_status VARCHAR(16) NOT NULL DEFAULT 'EMPTY',
    cover_image VARCHAR(1024) NULL,
    is_featured TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    seo_title_zh VARCHAR(255) NULL,
    seo_title_en VARCHAR(255) NULL,
    seo_description_zh VARCHAR(500) NULL,
    seo_description_en VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    active_product_code VARCHAR(100)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN product_code ELSE NULL END) STORED,
    active_slug VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN slug ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_product_featured CHECK (is_featured IN (0, 1)),
    CONSTRAINT chk_product_english_status CHECK (english_status IN ('EMPTY', 'AI_DRAFT', 'CONFIRMED')),
    CONSTRAINT chk_product_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    UNIQUE KEY uk_product_active_code (active_product_code),
    UNIQUE KEY uk_product_active_slug (active_slug),
    KEY idx_product_category_status_deleted_sort (category_id, status, deleted_at, sort_order),
    KEY idx_product_featured_status_sort (is_featured, status, sort_order),
    KEY idx_product_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE product_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    media_id BIGINT NULL,
    image_url VARCHAR(1024) NOT NULL,
    alt_zh VARCHAR(200) NULL,
    alt_en VARCHAR(200) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_cover TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_product_image_cover CHECK (is_cover IN (0, 1)),
    KEY idx_product_image_product_sort (product_id, sort_order, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE media_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    storage_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    public_url VARCHAR(1024) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    mime_type VARCHAR(127) NOT NULL,
    file_size BIGINT NOT NULL,
    width INT NULL,
    height INT NULL,
    checksum VARCHAR(128) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    active_storage_key VARCHAR(512)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN storage_key ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT chk_media_file_size CHECK (file_size >= 0),
    CONSTRAINT chk_media_file_dimensions CHECK ((width IS NULL OR width >= 0) AND (height IS NULL OR height >= 0)),
    CONSTRAINT chk_media_file_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    UNIQUE KEY uk_media_file_active_storage_key (active_storage_key),
    KEY idx_media_file_deleted (deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

ALTER TABLE product_image
    ADD CONSTRAINT fk_product_image_media FOREIGN KEY (media_id) REFERENCES media_file (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;
