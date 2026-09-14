package com.yongtuo.site.site;
import jakarta.validation.constraints.*;
public record ContactInput(@NotNull Type type,@Size(max=200) String labelZh,@Size(max=200) String labelEn,
        @NotBlank @Size(max=1000) String value,@Size(max=1000) String valueEn,@Size(max=2048) String linkUrl,
        @Min(0) int sortOrderZh,@Min(0) int sortOrderEn,boolean enabled) {
    public enum Type { PHONE,WECHAT,EMAIL,WHATSAPP,LINKEDIN,ADDRESS }
}
