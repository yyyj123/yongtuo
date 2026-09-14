package com.yongtuo.site.content.home;
import com.yongtuo.site.product.EnglishStatus;
import jakarta.validation.constraints.*;
public record BusinessEntryInput(@NotNull @Pattern(regexp="HARDWARE|MACHINED|CNC") String code,
        @Size(max=200) String titleZh,@Size(max=200) String titleEn,@Size(max=10000) String contentZh,@Size(max=10000) String contentEn,
        @Size(max=2048) String imageUrl,@Size(max=512) @Pattern(regexp="/(?!/)[^\\s]*") String linkPath,
        @NotNull EnglishStatus englishStatus,boolean enabled) { }
