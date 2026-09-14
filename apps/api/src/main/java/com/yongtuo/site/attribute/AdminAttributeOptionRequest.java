package com.yongtuo.site.attribute;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAttributeOptionRequest(
        Long id,
        @NotBlank @Size(max = 100) String valueCode,
        @NotBlank @Size(max = 200) String labelZh,
        @Size(max = 200) String labelEn,
        @NotNull @Min(0) Integer sortOrder,
        @NotNull AttributeStatus status) {}
