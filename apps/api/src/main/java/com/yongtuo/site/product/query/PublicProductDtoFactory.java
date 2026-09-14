package com.yongtuo.site.product.query;

import com.yongtuo.site.product.Product;
import java.util.List;

/** Keeps public product list/search projections on one DTO contract. */
public final class PublicProductDtoFactory {
    private PublicProductDtoFactory() {}

    public static PublicProductDto summary(Product product, boolean english) {
        return complete(product, english,
                new PublicProductCompositionAssembler.Composition(List.of(), List.of(), List.of()));
    }

    public static PublicProductDto complete(Product product, boolean english,
                                            PublicProductCompositionAssembler.Composition composition) {
        return complete(product, english, composition, null, List.of());
    }

    public static PublicProductDto complete(Product product, boolean english, PublicProductCompositionAssembler.Composition composition,
                                            String categorySlug, List<PublicProductDto.Image> images) {
        boolean confirmed = product.getEnglishStatus() == com.yongtuo.site.product.EnglishStatus.CONFIRMED;
        return new PublicProductDto(product.getProductCode(), product.getSlug(),
                english ? product.getNameEn() : product.getNameZh(),
                english ? product.getSummaryEn() : product.getSummaryZh(),
                english ? product.getDescriptionEn() : product.getDescriptionZh(), product.getCoverImage(),
                Boolean.TRUE.equals(product.getIsFeatured()),
                english ? product.getSeoTitleEn() : product.getSeoTitleZh(),
                english ? product.getSeoDescriptionEn() : product.getSeoDescriptionZh(),
                composition.attributes(), composition.variants(), composition.publicAttachments(), composition.relatedCases(), !english && confirmed ? product.getNameEn() : null, confirmed, categorySlug, images);
    }
}
