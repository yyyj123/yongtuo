package com.yongtuo.site.attribute;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CategoryAttributeBindingRequest(
        @NotNull Long attributeId,
        @NotNull Boolean isFilterable,
        @NotNull Boolean isRequired,
        @NotNull Boolean showInDetail,
        @NotNull @Min(0) Integer sortOrder) {}
