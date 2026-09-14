package com.yongtuo.site.product.query;

import java.util.List;

public record PublicProductPage(List<PublicProductDto> items, int page, int pageSize, long total, int totalPages) {
    public PublicProductPage {
        items = List.copyOf(items);
    }
}
