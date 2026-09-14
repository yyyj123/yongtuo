package com.yongtuo.site.catalog;
import com.yongtuo.site.content.LanguageMode;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;
import jakarta.validation.constraints.*;
public record CatalogInput(@Size(max=200) String titleZh,@Size(max=200) String titleEn,
        @NotNull LanguageMode languageMode,@NotNull EnglishStatus englishStatus,
        @NotBlank @Size(max=100) String version,@Size(max=2048) String coverImage,@Positive Long mediaId,
        @NotBlank @Size(max=2048) @Pattern(regexp="https?://[^\\s]+") String fileUrl,@NotNull ProductStatus status) { }
