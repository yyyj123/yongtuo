package com.yongtuo.site.translation;
import jakarta.validation.constraints.*;
import java.util.List;
public record DraftRequest(@NotNull TranslationResource resourceType,@Positive long resourceId,
        @NotEmpty @Size(max=10) List<@NotBlank String> fields,boolean replaceConfirmed) { }
