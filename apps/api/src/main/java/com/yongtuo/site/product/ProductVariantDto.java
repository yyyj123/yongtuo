package com.yongtuo.site.product;

import java.util.List;

public record ProductVariantDto(Long id, String variantCode, String nameZh, String nameEn,
                                int sortOrder, ProductStatus status, List<ProductAttributeDto> values) {
    public ProductVariantDto {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
