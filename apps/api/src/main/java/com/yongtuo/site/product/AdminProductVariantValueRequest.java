package com.yongtuo.site.product;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminProductVariantValueRequest(@NotNull Long attributeId, @Size(max = 20000) String valueZh,
                                              @Size(max = 20000) String valueEn,
                                              BigDecimal numericValue, Long optionId,
                                              List<Long> optionIds) {
    public AdminProductVariantValueRequest(Long attributeId, String valueZh, String valueEn,
                                           BigDecimal numericValue, Long optionId) {
        this(attributeId, valueZh, valueEn, numericValue, optionId, List.of());
    }

    public AdminProductVariantValueRequest {
        optionIds = optionIds == null ? List.of() : List.copyOf(optionIds);
    }
}
