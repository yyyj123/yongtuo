package com.yongtuo.site.site;
import com.yongtuo.site.product.EnglishStatus;
import jakarta.validation.constraints.*;
public record ConfigValueInput(@Size(max=100000) String valueZh,@Size(max=100000) String valueEn,@NotNull EnglishStatus englishStatus) { }
