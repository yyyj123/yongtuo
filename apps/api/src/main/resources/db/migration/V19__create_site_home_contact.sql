CREATE TABLE site_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL UNIQUE,
    value_zh MEDIUMTEXT NULL,
    value_en MEDIUMTEXT NULL,
    value_type VARCHAR(16) NOT NULL,
    english_status VARCHAR(16) NOT NULL DEFAULT 'EMPTY',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK(english_status IN ('EMPTY','AI_DRAFT','CONFIRMED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE contact_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(16) NOT NULL,
    label_zh VARCHAR(200) NULL,
    label_en VARCHAR(200) NULL,
    value VARCHAR(1000) NOT NULL,
    value_en VARCHAR(1000) NULL,
    link_url VARCHAR(2048) NULL,
    sort_order_zh INT NOT NULL DEFAULT 0,
    sort_order_en INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK(type IN ('PHONE','WECHAT','EMAIL','WHATSAPP','LINKEDIN','ADDRESS')),
    CHECK(sort_order_zh>=0 AND sort_order_en>=0),
    CHECK(enabled IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE home_section (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(32) NOT NULL UNIQUE,
    title_zh VARCHAR(200) NULL,
    title_en VARCHAR(200) NULL,
    subtitle_zh VARCHAR(500) NULL,
    subtitle_en VARCHAR(500) NULL,
    content_zh MEDIUMTEXT NULL,
    content_en MEDIUMTEXT NULL,
    image_url VARCHAR(2048) NULL,
    english_status VARCHAR(16) NOT NULL DEFAULT 'EMPTY',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK(section_code IN ('HERO','BUSINESS','CNC','CAPABILITIES','ABOUT')),
    CHECK(english_status IN ('EMPTY','AI_DRAFT','CONFIRMED')),
    CHECK(enabled IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO home_section(section_code,sort_order) VALUES ('HERO',1),('BUSINESS',2),('CNC',5),('CAPABILITIES',6),('ABOUT',10);
CREATE TABLE home_business_entry (
    code VARCHAR(32) PRIMARY KEY,
    title_zh VARCHAR(200) NULL,title_en VARCHAR(200) NULL,
    content_zh TEXT NULL,content_en TEXT NULL,
    image_url VARCHAR(2048) NULL,link_path VARCHAR(512) NULL,
    english_status VARCHAR(16) NOT NULL DEFAULT 'EMPTY',enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL,
    CHECK(code IN ('HARDWARE','MACHINED','CNC')),
    CHECK(english_status IN ('EMPTY','AI_DRAFT','CONFIRMED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO home_business_entry(code,sort_order) VALUES ('HARDWARE',1),('MACHINED',2),('CNC',3);
CREATE TABLE home_featured_product (resource_id BIGINT PRIMARY KEY,sort_order INT NOT NULL,FOREIGN KEY(resource_id) REFERENCES product(id)) ENGINE=InnoDB;
CREATE TABLE home_featured_case (resource_id BIGINT PRIMARY KEY,sort_order INT NOT NULL,FOREIGN KEY(resource_id) REFERENCES case_study(id)) ENGINE=InnoDB;
CREATE TABLE home_featured_article (resource_id BIGINT PRIMARY KEY,sort_order INT NOT NULL,FOREIGN KEY(resource_id) REFERENCES article(id)) ENGINE=InnoDB;
CREATE TABLE home_featured_certificate (resource_id BIGINT PRIMARY KEY,sort_order INT NOT NULL,FOREIGN KEY(resource_id) REFERENCES certificate(id)) ENGINE=InnoDB;
