ALTER TABLE product_variant_value
    ADD CONSTRAINT fk_product_variant_value_attribute FOREIGN KEY (attribute_id)
        REFERENCES attribute_definition (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_product_variant_value_option_attribute
        FOREIGN KEY (option_id, attribute_id) REFERENCES attribute_option (id, attribute_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;
