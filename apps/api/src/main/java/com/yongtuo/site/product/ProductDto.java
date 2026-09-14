package com.yongtuo.site.product;

import java.util.List;

public record ProductDto(Long id, Long categoryId, String productCode, String slug,
        String nameZh, String nameEn, String summaryZh, String summaryEn,
        String descriptionZh, String descriptionEn, EnglishStatus englishStatus, String coverImage, boolean isFeatured,
        int sortOrder, ProductStatus status, String seoTitleZh, String seoTitleEn,
        String seoDescriptionZh, String seoDescriptionEn,
        List<ProductAttributeDto> attributes, List<ProductVariantDto> variants) {
    public ProductDto(Long id, Long categoryId, String productCode, String slug,
                      String nameZh, String nameEn, String summaryZh, String summaryEn,
                      String descriptionZh, String descriptionEn, EnglishStatus englishStatus, String coverImage,
                      boolean isFeatured, int sortOrder, ProductStatus status, String seoTitleZh, String seoTitleEn,
                      String seoDescriptionZh, String seoDescriptionEn) {
        this(id, categoryId, productCode, slug, nameZh, nameEn, summaryZh, summaryEn, descriptionZh, descriptionEn,
                englishStatus, coverImage, isFeatured, sortOrder, status, seoTitleZh, seoTitleEn, seoDescriptionZh,
                seoDescriptionEn, List.of(), List.of());
    }

    public ProductDto {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
        variants = variants == null ? List.of() : List.copyOf(variants);
    }
}
