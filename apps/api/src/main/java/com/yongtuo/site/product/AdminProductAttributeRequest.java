package com.yongtuo.site.product;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Data-driven value submitted for a product attribute. */
public record AdminProductAttributeRequest(@NotNull Long attributeId, @Size(max = 20000) String valueZh,
                                           @Size(max = 20000) String valueEn,
                                           BigDecimal numericValue, Long optionId,
                                           List<Long> optionIds, @Min(0) Integer sortOrder) {
    public AdminProductAttributeRequest(Long attributeId, String valueZh, String valueEn,
                                        BigDecimal numericValue, Long optionId, Integer sortOrder) {
        this(attributeId, valueZh, valueEn, numericValue, optionId, List.of(), sortOrder);
    }

    public AdminProductAttributeRequest {
        optionIds = optionIds == null ? List.of() : List.copyOf(optionIds);
        sortOrder = sortOrder == null ? 0 : sortOrder;
    }
}
