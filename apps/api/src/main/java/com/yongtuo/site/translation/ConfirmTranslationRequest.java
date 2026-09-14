package com.yongtuo.site.translation;
import jakarta.validation.constraints.*;
import java.util.Map;
public record ConfirmTranslationRequest(@NotNull TranslationResource resourceType,@Positive long resourceId,
        @NotEmpty @Size(max=10) Map<@NotBlank String,@NotNull String> expectedFields) { }
