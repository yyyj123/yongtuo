package com.yongtuo.site.article;

import java.time.LocalDateTime;

public record PublicArticle(long id, long categoryId, String slug, String title, String coverImage,
        String summary, String content, String seoTitle, String seoDescription, LocalDateTime publishedAt) {
    static PublicArticle from(Article a, boolean en) {
        return new PublicArticle(a.id(), a.categoryId(), a.slug(), en ? a.titleEn() : a.titleZh(), a.coverImage(),
                en ? a.summaryEn() : a.summaryZh(), en ? a.contentEn() : a.contentZh(),
                en ? a.seoTitleEn() : a.seoTitleZh(), en ? a.seoDescriptionEn() : a.seoDescriptionZh(), a.publishedAt());
    }
}
