package com.yongtuo.site.content.home;
import com.yongtuo.site.product.EnglishStatus;
import jakarta.validation.constraints.*;
public record HomeSectionInput(@Size(max=200) String titleZh,@Size(max=200) String titleEn,
        @Size(max=500) String subtitleZh,@Size(max=500) String subtitleEn,
        @Size(max=1000000) String contentZh,@Size(max=1000000) String contentEn,
        @Size(max=2048) String imageUrl,@NotNull EnglishStatus englishStatus,boolean enabled) { }
