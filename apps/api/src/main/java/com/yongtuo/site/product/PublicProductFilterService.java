package com.yongtuo.site.product;

import com.yongtuo.site.attribute.CategoryAttributeService;
import com.yongtuo.site.category.CategoryService;
import com.yongtuo.site.common.ApiResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PublicProductFilterService {
    private final CategoryService categories;
    private final CategoryAttributeService attributes;
    private final JdbcTemplate jdbc;
    public PublicProductFilterService(CategoryService categories, CategoryAttributeService attributes, JdbcTemplate jdbc) {
        this.categories = categories; this.attributes = attributes; this.jdbc = jdbc;
    }
    public record Option(String value, String label) {}
    public record Filter(String code, String name, String dataType, String unit, List<Option> options) {}
    private static String label(String value, String code) { return value == null || value.isBlank() ? code : value; }

    public ApiResponse<List<Filter>> filters(String category, Locale locale) {
        boolean en = locale != null && "en".equals(locale.getLanguage());
        if (category != null && !category.isBlank()) {
            long id = categories.getPublicBySlug(category, locale).id();
            return ApiResponse.success(attributes.getCategoryAttributes(id).stream().filter(a -> a.isFilterable())
                    .map(a -> new Filter(a.code(), label(en ? a.nameEn() : a.nameZh(), a.code()), a.dataType().name(), a.unit(),
                            a.options().stream().map(o -> new Option(o.valueCode(), label(en ? o.labelEn() : o.labelZh(), o.valueCode()))).toList())).toList());
        }
        return ApiResponse.success(jdbc.query("SELECT * FROM attribute_definition WHERE status = 'ACTIVE' AND is_global = 1 AND default_filterable = 1 ORDER BY sort_order, id",
                (rs, row) -> new Filter(rs.getString("code"), label(rs.getString(en ? "name_en" : "name_zh"), rs.getString("code")),
                        rs.getString("data_type"), rs.getString("unit"), jdbc.query("SELECT * FROM attribute_option WHERE attribute_id = ? AND status = 'ACTIVE' ORDER BY sort_order, id",
                                (option, n) -> new Option(option.getString("value_code"), label(option.getString(en ? "label_en" : "label_zh"), option.getString("value_code"))), rs.getLong("id")))));
    }
}
