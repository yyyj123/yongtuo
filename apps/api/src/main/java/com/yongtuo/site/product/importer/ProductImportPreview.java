package com.yongtuo.site.product.importer;

import java.util.List;

public record ProductImportPreview(
        String importToken,
        int total,
        int valid,
        int warning,
        int error,
        List<ProductImportRowResult> rows) {

    public ProductImportPreview {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
