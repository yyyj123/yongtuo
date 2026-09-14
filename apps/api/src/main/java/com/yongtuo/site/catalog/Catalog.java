package com.yongtuo.site.catalog;
import com.yongtuo.site.content.LanguageMode;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;
public record Catalog(long id,String titleZh,String titleEn,LanguageMode languageMode,EnglishStatus englishStatus,
        String version,String coverImage,Long mediaId,String fileUrl,boolean isPrimary,ProductStatus status) { }
