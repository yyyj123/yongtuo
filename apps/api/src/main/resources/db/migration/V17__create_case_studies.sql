CREATE TABLE case_study (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(191) NOT NULL UNIQUE,
    title_zh VARCHAR(200) NULL,
    title_en VARCHAR(200) NULL,
    cover_image VARCHAR(2048) NULL,
    summary_zh TEXT NULL,
    summary_en TEXT NULL,
    content_zh MEDIUMTEXT NULL,
    content_en MEDIUMTEXT NULL,
    language_mode VARCHAR(16) NOT NULL DEFAULT 'ZH_ONLY',
    english_status VARCHAR(16) NOT NULL DEFAULT 'EMPTY',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    seo_title_zh VARCHAR(200) NULL,
    seo_title_en VARCHAR(200) NULL,
    seo_description_zh VARCHAR(500) NULL,
    seo_description_en VARCHAR(500) NULL,
    published_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    application_scene_zh TEXT NULL,
    application_scene_en TEXT NULL,
    requirement_zh TEXT NULL,
    requirement_en TEXT NULL,
    solution_zh TEXT NULL,
    solution_en TEXT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT ck_case_study_language CHECK (language_mode IN ('ZH_ONLY', 'EN_ONLY', 'BILINGUAL')),
    CONSTRAINT ck_case_study_english CHECK (english_status IN ('EMPTY', 'AI_DRAFT', 'CONFIRMED')),
    CONSTRAINT ck_case_study_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    CONSTRAINT ck_case_study_featured CHECK (is_featured IN (0,1)),
    CONSTRAINT ck_case_study_sort CHECK (sort_order >= 0),
    KEY idx_case_study_public (status, deleted_at, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE case_product (
    case_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY(case_id,product_id),
    KEY idx_case_product_reverse(product_id,case_id),
    FOREIGN KEY(case_id) REFERENCES case_study(id) ON DELETE CASCADE,
    FOREIGN KEY(product_id) REFERENCES product(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE case_category (
    case_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY(case_id,category_id),
    KEY idx_case_category_reverse(category_id,case_id),
    FOREIGN KEY(case_id) REFERENCES case_study(id) ON DELETE CASCADE,
    FOREIGN KEY(category_id) REFERENCES product_category(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE case_image (
    case_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    PRIMARY KEY(case_id,sort_order),
    FOREIGN KEY(case_id) REFERENCES case_study(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
