package com.yongtuo.site.product;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminProductVariantRequest(@NotBlank @Size(max = 100) String variantCode,
                                         @NotBlank @Size(max = 200) String nameZh,
                                         @Size(max = 200) String nameEn,
                                         @Min(0) Integer sortOrder, @NotNull ProductStatus status,
                                         List<@Valid AdminProductVariantValueRequest> values) {
    public AdminProductVariantRequest {
        sortOrder = sortOrder == null ? 0 : sortOrder;
        values = values == null ? List.of() : List.copyOf(values);
    }
}
