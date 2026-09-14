package com.yongtuo.site.product.query;

import java.util.List;

public record PublicProductVariantDto(String variantCode, String name, List<PublicProductAttributeDto> values) {
    public PublicProductVariantDto {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
