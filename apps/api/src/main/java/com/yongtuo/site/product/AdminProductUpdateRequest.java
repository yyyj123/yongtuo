package com.yongtuo.site.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import jakarta.validation.Valid;

public record AdminProductUpdateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 100) String productCode,
        @NotBlank @Size(max = 191) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
        @NotBlank @Size(max = 200) String nameZh,
        @Size(max = 200) String nameEn,
        String summaryZh, String summaryEn, String descriptionZh, String descriptionEn,
        @NotNull EnglishStatus englishStatus,
        @Size(max = 1024) String coverImage,
        @NotNull Boolean isFeatured,
        @NotNull @Min(0) Integer sortOrder,
        @Size(max = 255) String seoTitleZh, @Size(max = 255) String seoTitleEn,
        @Size(max = 500) String seoDescriptionZh, @Size(max = 500) String seoDescriptionEn,
        @NotNull ProductStatus status,
        List<@Valid AdminProductAttributeRequest> attributes, List<@Valid AdminProductVariantRequest> variants) {
    public AdminProductUpdateRequest(Long categoryId, String productCode, String slug, String nameZh, String nameEn,
                                     String summaryZh, String summaryEn, String descriptionZh, String descriptionEn,
                                     EnglishStatus englishStatus, String coverImage, Boolean isFeatured, Integer sortOrder,
                                     String seoTitleZh, String seoTitleEn, String seoDescriptionZh, String seoDescriptionEn,
                                     ProductStatus status) {
        this(categoryId, productCode, slug, nameZh, nameEn, summaryZh, summaryEn, descriptionZh, descriptionEn,
                englishStatus, coverImage, isFeatured, sortOrder, seoTitleZh, seoTitleEn, seoDescriptionZh,
                seoDescriptionEn, status, List.of(), List.of());
    }

    public AdminProductUpdateRequest {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
        variants = variants == null ? List.of() : List.copyOf(variants);
    }
}
