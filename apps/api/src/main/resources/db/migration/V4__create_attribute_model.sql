CREATE TABLE attribute_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name_zh VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NULL,
    code VARCHAR(100) NOT NULL,
    data_type VARCHAR(16) NOT NULL,
    unit VARCHAR(64) NULL,
    is_global TINYINT NOT NULL DEFAULT 0,
    default_filterable TINYINT NOT NULL DEFAULT 0,
    default_required TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    CONSTRAINT chk_attribute_definition_data_type
        CHECK (data_type IN ('TEXT', 'NUMBER', 'SELECT', 'MULTI_SELECT')),
    CONSTRAINT chk_attribute_definition_global CHECK (is_global IN (0, 1)),
    CONSTRAINT chk_attribute_definition_filterable CHECK (default_filterable IN (0, 1)),
    CONSTRAINT chk_attribute_definition_required CHECK (default_required IN (0, 1)),
    CONSTRAINT chk_attribute_definition_sort_order CHECK (sort_order >= 0),
    CONSTRAINT chk_attribute_definition_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    UNIQUE KEY uk_attribute_definition_code (code),
    KEY idx_attribute_definition_status_sort (status, sort_order, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE attribute_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attribute_id BIGINT NOT NULL,
    value_code VARCHAR(100) NOT NULL,
    label_zh VARCHAR(200) NOT NULL,
    label_en VARCHAR(200) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    CONSTRAINT fk_attribute_option_definition FOREIGN KEY (attribute_id) REFERENCES attribute_definition (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_attribute_option_sort_order CHECK (sort_order >= 0),
    CONSTRAINT chk_attribute_option_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    UNIQUE KEY uk_attribute_option_attribute_value (attribute_id, value_code),
    UNIQUE KEY uk_attribute_option_id_attribute (id, attribute_id),
    KEY idx_attribute_option_attribute_sort (attribute_id, status, sort_order, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE category_attribute (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    is_filterable TINYINT NOT NULL DEFAULT 0,
    is_required TINYINT NOT NULL DEFAULT 0,
    show_in_detail TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_category_attribute_category FOREIGN KEY (category_id) REFERENCES product_category (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_category_attribute_definition FOREIGN KEY (attribute_id) REFERENCES attribute_definition (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_category_attribute_filterable CHECK (is_filterable IN (0, 1)),
    CONSTRAINT chk_category_attribute_required CHECK (is_required IN (0, 1)),
    CONSTRAINT chk_category_attribute_detail CHECK (show_in_detail IN (0, 1)),
    CONSTRAINT chk_category_attribute_sort_order CHECK (sort_order >= 0),
    UNIQUE KEY uk_category_attribute_category_attribute (category_id, attribute_id),
    KEY idx_category_attribute_attribute_category (attribute_id, category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE product_attribute_value (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    value_zh TEXT NULL,
    value_en TEXT NULL,
    numeric_value DECIMAL(20, 6) NULL,
    option_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_attribute_value_product FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_product_attribute_value_attribute FOREIGN KEY (attribute_id) REFERENCES attribute_definition (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_product_attribute_value_option_attribute
        FOREIGN KEY (option_id, attribute_id) REFERENCES attribute_option (id, attribute_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_product_attribute_value_sort_order CHECK (sort_order >= 0),
    UNIQUE KEY uk_product_attribute_value_product_attribute (product_id, attribute_id),
    KEY idx_product_attribute_value_attribute_numeric (attribute_id, numeric_value),
    KEY idx_product_attribute_value_attribute_option (attribute_id, option_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
