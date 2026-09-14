package com.yongtuo.site.product.importer;

import java.util.List;

public record BatchImageReport(
        int total,
        int success,
        int failed,
        int unmatched,
        List<BatchImageFileResult> files) {
    public BatchImageReport {
        files = List.copyOf(files);
    }
}
