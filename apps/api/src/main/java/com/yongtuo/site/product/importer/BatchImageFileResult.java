package com.yongtuo.site.product.importer;

public record BatchImageFileResult(
        String fileName,
        String productCode,
        Integer sequence,
        BatchImageFileStatus status,
        String reason) {}
