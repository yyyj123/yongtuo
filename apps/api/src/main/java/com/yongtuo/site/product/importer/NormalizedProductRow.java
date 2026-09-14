package com.yongtuo.site.product.importer;

import com.yongtuo.site.product.ProductStatus;

public record NormalizedProductRow(
        int rowNumber,
        long categoryId,
        String productCode,
        String slug,
        String nameZh,
        ProductStatus status) {}
