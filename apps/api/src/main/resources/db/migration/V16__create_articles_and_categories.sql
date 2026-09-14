CREATE TABLE article_category (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name_zh VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NULL,
    slug VARCHAR(191) NOT NULL UNIQUE,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT ck_article_category_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_article_category_sort CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE article (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
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
    deleted_at DATETIME NULL,
    CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES article_category(id),
    CONSTRAINT ck_article_language CHECK (language_mode IN ('ZH_ONLY', 'EN_ONLY', 'BILINGUAL')),
    CONSTRAINT ck_article_english CHECK (english_status IN ('EMPTY', 'AI_DRAFT', 'CONFIRMED')),
    CONSTRAINT ck_article_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    CONSTRAINT ck_article_featured CHECK (is_featured IN (0,1)),
    CONSTRAINT ck_article_sort CHECK (sort_order >= 0),
    KEY idx_article_public (status, deleted_at, sort_order, id),
    KEY idx_article_category (category_id, status, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO article_category(name_zh, name_en, slug, sort_order) VALUES
('公司动态', 'Company News', 'company-news', 0),
('产品知识', 'Product Knowledge', 'product-knowledge', 1),
('CNC 加工知识', 'CNC Machining Knowledge', 'cnc-knowledge', 2),
('行业应用', 'Industry Applications', 'industry-applications', 3);
