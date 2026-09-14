package com.yongtuo.site.certificate;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;
public record Certificate(long id,String type,String nameZh,String nameEn,String descriptionZh,String descriptionEn,
        EnglishStatus englishStatus,String coverImage,Long mediaId,String fileUrl,boolean isPublic,
        boolean allowDownload,boolean isFeatured,int sortOrder,ProductStatus status) { }
