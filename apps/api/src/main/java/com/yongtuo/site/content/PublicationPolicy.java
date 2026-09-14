package com.yongtuo.site.content;

import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;

public final class PublicationPolicy {
    private PublicationPolicy() { }
    public static boolean visible(LanguageMode mode, EnglishStatus english, ProductStatus status, boolean en) {
        return status == ProductStatus.PUBLISHED && (en
                ? mode != LanguageMode.ZH_ONLY && english == EnglishStatus.CONFIRMED
                : mode != LanguageMode.EN_ONLY);
    }
    public static String publicPredicate(boolean english) {
        return "status='PUBLISHED' AND deleted_at IS NULL AND " + (english
                ? "language_mode IN ('EN_ONLY','BILINGUAL') AND english_status='CONFIRMED'"
                : "language_mode IN ('ZH_ONLY','BILINGUAL')");
    }
}
