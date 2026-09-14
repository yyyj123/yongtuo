ALTER TABLE product_attribute_value
    ADD COLUMN value_key BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_product_attribute_value_value_key CHECK (value_key >= 0);

UPDATE product_attribute_value SET value_key = COALESCE(option_id, 0);

ALTER TABLE product_attribute_value
    ADD CONSTRAINT uk_product_attribute_value_product_attribute_value_key
        UNIQUE (product_id, attribute_id, value_key);

ALTER TABLE product_variant_value
    ADD COLUMN value_key BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_product_variant_value_value_key CHECK (value_key >= 0);

UPDATE product_variant_value SET value_key = COALESCE(option_id, 0);

ALTER TABLE product_variant_value
    ADD CONSTRAINT uk_product_variant_value_variant_attribute_value_key
        UNIQUE (variant_id, attribute_id, value_key);
