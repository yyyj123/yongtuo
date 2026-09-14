package com.yongtuo.site.article;
import jakarta.validation.constraints.*;
public record ArticleCategoryInput(@NotBlank @Size(max=200) String nameZh,
        @Size(max=200) String nameEn,
        @NotBlank @Size(max=191) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
        @Min(0) int sortOrder, @NotNull @Pattern(regexp="ACTIVE|INACTIVE") String status) { }
