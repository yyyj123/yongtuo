package com.yongtuo.site.attribute;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminAttributeWriteRequest(
        @NotBlank @Size(max = 200) String nameZh,
        @Size(max = 200) String nameEn,
        @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z][a-z0-9_-]*") String code,
        @NotNull AttributeDataType dataType,
        @Size(max = 64) String unit,
        @NotNull Boolean isGlobal,
        @NotNull Boolean defaultFilterable,
        @NotNull Boolean defaultRequired,
        @NotNull @Min(0) Integer sortOrder,
        @NotNull AttributeStatus status,
        List<@Valid AdminAttributeOptionRequest> options) {}
