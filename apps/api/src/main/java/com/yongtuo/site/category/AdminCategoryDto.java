package com.yongtuo.site.category;

import java.util.List;

public record AdminCategoryDto(
        long id,
        Long parentId,
        String nameZh,
        String nameEn,
        String slug,
        String coverImage,
        String descriptionZh,
        String descriptionEn,
        CategoryMode categoryMode,
        int sortOrder,
        CategoryStatus status,
        boolean showOnHome,
        String seoTitleZh,
        String seoTitleEn,
        String seoDescriptionZh,
        String seoDescriptionEn,
        List<AdminCategoryDto> children) {}
