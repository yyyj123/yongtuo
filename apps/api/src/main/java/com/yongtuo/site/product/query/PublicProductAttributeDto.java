package com.yongtuo.site.product.query;

import com.yongtuo.site.attribute.AttributeDataType;
import java.math.BigDecimal;
import java.util.List;

public record PublicProductAttributeDto(String code, String name, AttributeDataType dataType, String unit,
                                        String value, BigDecimal numericValue,
                                        List<PublicProductOptionDto> options) {
    public PublicProductAttributeDto {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
