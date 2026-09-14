package com.yongtuo.site.casestudy;

import com.yongtuo.site.content.LanguageMode;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;
import java.time.LocalDateTime;

public record CaseStudy(long id, String slug, String titleZh, String titleEn,
        String coverImage, String summaryZh, String summaryEn, String contentZh, String contentEn,
        LanguageMode languageMode, EnglishStatus englishStatus, ProductStatus status,
        boolean isFeatured, int sortOrder, String seoTitleZh, String seoTitleEn,
        String seoDescriptionZh, String seoDescriptionEn, LocalDateTime publishedAt,
        LocalDateTime createdAt, LocalDateTime updatedAt,
        String applicationSceneZh, String applicationSceneEn, String requirementZh, String requirementEn, String solutionZh, String solutionEn) { }
