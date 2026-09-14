package com.yongtuo.site.product.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProductFilter(String category, Map<String, List<String>> attributes,
                            int page, int pageSize, String sort) {
    public ProductFilter(String category, Map<String, List<String>> attributes,
                         Integer page, Integer pageSize, String sort) {
        this(clean(category), copy(attributes), page == null ? 1 : page, pageSize == null ? 24 : pageSize, clean(sort));
    }

    public ProductFilter {
        category = clean(category);
        attributes = copy(attributes);
        sort = clean(sort);
        if (page < 1 || pageSize < 1 || pageSize > 100) throw ProductQueryException.invalidFilter();
    }

    private static Map<String, List<String>> copy(Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) return Map.of();
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            String key = clean(entry.getKey());
            List<String> values = entry.getValue() == null ? List.of() : entry.getValue().stream()
                    .map(ProductFilter::clean).toList();
            if (key == null || values.isEmpty() || values.stream().anyMatch(value -> value == null)) {
                throw ProductQueryException.invalidFilter();
            }
            result.put(key, values);
        }
        return Map.copyOf(result);
    }

    private static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
