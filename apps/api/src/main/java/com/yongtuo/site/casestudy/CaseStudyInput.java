package com.yongtuo.site.casestudy;

import com.yongtuo.site.content.LanguageMode;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;
import jakarta.validation.constraints.*;

public record CaseStudyInput(
        @NotBlank @Size(max=191) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
        @Size(max=200) String titleZh, @Size(max=200) String titleEn,
        @Size(max=2048) String coverImage,
        @Size(max=10000) String summaryZh, @Size(max=10000) String summaryEn,
        @Size(max=1000000) String contentZh, @Size(max=1000000) String contentEn,
        @NotNull LanguageMode languageMode, @NotNull EnglishStatus englishStatus,
        @NotNull ProductStatus status, boolean isFeatured, @Min(0) int sortOrder,
        @Size(max=200) String seoTitleZh, @Size(max=200) String seoTitleEn,
        @Size(max=500) String seoDescriptionZh, @Size(max=500) String seoDescriptionEn,
        @Size(max=10000) String applicationSceneZh, @Size(max=10000) String applicationSceneEn, @Size(max=10000) String requirementZh, @Size(max=10000) String requirementEn, @Size(max=10000) String solutionZh, @Size(max=10000) String solutionEn, @Size(max=100) java.util.List<@NotNull @Positive Long> productIds, @Size(max=100) java.util.List<@NotNull @Positive Long> categoryIds, @Size(max=50) java.util.List<@NotBlank @Size(max=2048) String> imageUrls) { }
