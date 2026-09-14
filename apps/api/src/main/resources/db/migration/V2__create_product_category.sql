CREATE TABLE product_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NULL,
    name_zh VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NULL,
    slug VARCHAR(191) NOT NULL,
    cover_image VARCHAR(1024) NULL,
    description_zh TEXT NULL,
    description_en TEXT NULL,
    category_mode VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    show_on_home TINYINT NOT NULL DEFAULT 0,
    seo_title_zh VARCHAR(255) NULL,
    seo_title_en VARCHAR(255) NULL,
    seo_description_zh VARCHAR(500) NULL,
    seo_description_en VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    active_slug VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN slug ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_category_parent
        FOREIGN KEY (parent_id) REFERENCES product_category (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_product_category_mode
        CHECK (category_mode IN ('NORMAL', 'SHOWCASE')),
    CONSTRAINT chk_product_category_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    UNIQUE KEY uk_product_category_active_slug (active_slug),
    KEY idx_product_category_parent_sort (parent_id, sort_order, id),
    KEY idx_product_category_status_deleted (status, deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
