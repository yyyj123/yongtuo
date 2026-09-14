package com.yongtuo.site.product.importer;

import java.util.List;

public record ProductImportRowResult(
        int rowNumber,
        String productCode,
        String slug,
        String categorySlug,
        String nameZh,
        String status,
        ImportRowLevel level,
        List<String> messages) {

    public ProductImportRowResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
