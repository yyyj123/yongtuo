package com.yongtuo.site.product;

import com.yongtuo.site.attribute.AttributeDataType;
import java.math.BigDecimal;
import java.util.List;

public record ProductAttributeDto(Long attributeId, String code, String nameZh, String nameEn,
                                  AttributeDataType dataType, String unit, String valueZh, String valueEn,
                                  BigDecimal numericValue, Long optionId, List<Long> optionIds, int sortOrder) {
    public ProductAttributeDto {
        optionIds = optionIds == null ? List.of() : List.copyOf(optionIds);
    }
}
