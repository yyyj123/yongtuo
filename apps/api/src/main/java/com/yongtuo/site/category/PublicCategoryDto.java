package com.yongtuo.site.category;

import java.util.List;

public record PublicCategoryDto(
        long id,
        String slug,
        String name,
        String coverImage,
        String description,
        CategoryMode mode,
        boolean showOnHome,
        String seoTitle,
        String seoDescription,
        List<PublicCategoryDto> children) {}
