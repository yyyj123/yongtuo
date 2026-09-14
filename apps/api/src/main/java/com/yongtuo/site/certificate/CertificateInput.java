package com.yongtuo.site.certificate;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;
import jakarta.validation.constraints.*;
public record CertificateInput(@NotBlank @Size(max=64) String type,
        @NotBlank @Size(max=200) String nameZh,@Size(max=200) String nameEn,
        @Size(max=10000) String descriptionZh,@Size(max=10000) String descriptionEn,
        @NotNull EnglishStatus englishStatus,@Size(max=2048) String coverImage,
        @Positive Long mediaId,@Size(max=2048) String fileUrl,boolean isPublic,boolean allowDownload,
        boolean isFeatured,@Min(0) int sortOrder,@NotNull ProductStatus status) { }
