package com.yongtuo.site.content;
import java.util.List;
public record ContentPage<T>(List<T> items, int page, int pageSize, long total, long totalPages) {
    public ContentPage { items = List.copyOf(items); }
}
