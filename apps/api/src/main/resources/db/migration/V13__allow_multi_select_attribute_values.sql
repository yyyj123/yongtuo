CREATE INDEX idx_product_attribute_value_product_attribute_id
    ON product_attribute_value (product_id, attribute_id, id);
DROP INDEX uk_product_attribute_value_product_attribute ON product_attribute_value;

CREATE INDEX idx_product_variant_value_variant_attribute_id
    ON product_variant_value (variant_id, attribute_id, id);
DROP INDEX uk_product_variant_value_attribute ON product_variant_value;
