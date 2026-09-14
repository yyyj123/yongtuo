package com.yongtuo.site.category;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CategorySortItem(@NotNull Long id, Long parentId, @NotNull @Min(0) Integer sortOrder) {}
