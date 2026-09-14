package com.yongtuo.site.product.importer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImportConfirmRequest(@NotBlank @Size(max = 64) String importToken) {}
