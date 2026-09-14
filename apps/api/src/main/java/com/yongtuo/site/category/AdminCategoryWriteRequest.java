package com.yongtuo.site.category;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCategoryWriteRequest(
        Long parentId,
        @NotBlank @Size(max = 200) String nameZh,
        @Size(max = 200) String nameEn,
        @NotBlank @Size(max = 191) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
        @Size(max = 1024) String coverImage,
        String descriptionZh,
        String descriptionEn,
        @NotNull CategoryMode categoryMode,
        @NotNull @Min(0) Integer sortOrder,
        @NotNull CategoryStatus status,
        @NotNull Boolean showOnHome,
        @Size(max = 255) String seoTitleZh,
        @Size(max = 255) String seoTitleEn,
        @Size(max = 500) String seoDescriptionZh,
        @Size(max = 500) String seoDescriptionEn) {}
