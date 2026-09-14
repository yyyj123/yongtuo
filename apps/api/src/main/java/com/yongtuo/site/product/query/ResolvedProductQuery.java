package com.yongtuo.site.product.query;

import java.util.List;

record ResolvedProductQuery(String category, List<Condition> conditions, int pageSize, long offset,
                            boolean english, boolean newest, String keyword) {
    record Condition(long attributeId, boolean numeric, boolean select, boolean fallbackFilterable, List<?> values) {
        Condition { values = List.copyOf(values); }
    }
}
