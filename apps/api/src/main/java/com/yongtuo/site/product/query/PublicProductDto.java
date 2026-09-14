package com.yongtuo.site.product.query;

import java.util.List;

public record PublicProductDto(String productCode, String slug, String name, String summary, String description, String coverImage,
                               boolean featured, String seoTitle, String seoDescription,
                               List<PublicProductAttributeDto> attributes, List<PublicProductVariantDto> variants,
                               List<PublicProductAttachmentDto> publicAttachments,
                               List<com.yongtuo.site.casestudy.CaseSummary> relatedCases, String secondaryName, boolean englishAvailable, String categorySlug, List<Image> images) {
    public record Image(String url, String alt) {}
    public PublicProductDto {
        images = images == null ? List.of() : List.copyOf(images);
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
        variants = variants == null ? List.of() : List.copyOf(variants);
        publicAttachments = publicAttachments == null ? List.of() : List.copyOf(publicAttachments);
        relatedCases = relatedCases == null ? List.of() : List.copyOf(relatedCases);
    }
}
