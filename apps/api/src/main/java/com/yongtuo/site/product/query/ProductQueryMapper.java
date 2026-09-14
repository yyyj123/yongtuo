package com.yongtuo.site.product.query;

import com.yongtuo.site.product.Product;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

@Mapper
public interface ProductQueryMapper {
    @SelectProvider(type = Sql.class, method = "list")
    List<Product> list(@Param("query") ResolvedProductQuery query);

    @SelectProvider(type = Sql.class, method = "count")
    long count(@Param("query") ResolvedProductQuery query);

    @SelectProvider(type = Sql.class, method = "detail")
    Product detail(@Param("slug") String slug, @Param("english") boolean english);

    final class Sql {
        private Sql() {}

        public static String list(Map<String, Object> parameters) {
            ResolvedProductQuery query = (ResolvedProductQuery) parameters.get("query");
            return select() + where(query) + (query.newest()
                    ? " ORDER BY p.created_at DESC, p.id DESC"
                    : " ORDER BY p.sort_order ASC, p.id ASC")
                    + " LIMIT #{query.pageSize} OFFSET #{query.offset}";
        }

        public static String count(Map<String, Object> parameters) {
            return "SELECT COUNT(*)" + from() + where((ResolvedProductQuery) parameters.get("query"));
        }

        public static String detail() {
            return select() + baseVisibility("#{english}") + " AND p.slug = #{slug}";
        }

        private static String select() { return "SELECT p.*" + from(); }

        private static String from() {
            return " FROM product p JOIN product_category c ON c.id = p.category_id";
        }

        private static String where(ResolvedProductQuery query) {
            StringBuilder sql = new StringBuilder(baseVisibility("#{query.english}"));
            if (query.keyword() != null) sql.append(" AND p.id IN (SELECT p.id ").append(
                    com.yongtuo.site.search.SearchMapper.FROM_AND_WHERE
                            .replace("#{english}", "#{query.english}").replace("#{keyword}", "#{query.keyword}")).append(")");
            if (query.category() != null) sql.append(" AND c.slug = #{query.category}");
            for (int i = 0; i < query.conditions().size(); i++) {
                ResolvedProductQuery.Condition condition = query.conditions().get(i);
                sql.append(" AND EXISTS (SELECT 1 FROM product_attribute_value pav");
                if (condition.select()) sql.append(" JOIN attribute_option ao ON ao.id = pav.option_id AND ao.attribute_id = pav.attribute_id AND ao.status = 'ACTIVE'");
                sql.append(" WHERE pav.product_id = p.id AND pav.attribute_id = #{query.conditions[").append(i).append("].attributeId} AND (");
                for (int j = 0; j < condition.values().size(); j++) {
                    if (j > 0) sql.append(" OR ");
                    String parameter = "#{query.conditions[" + i + "].values[" + j + "]}";
                    if (condition.numeric()) sql.append("pav.numeric_value = ").append(parameter);
                    else if (condition.select()) sql.append("ao.value_code = ").append(parameter);
                    else sql.append("(pav.value_zh = ").append(parameter).append(" OR pav.value_en = ").append(parameter).append(")");
                }
                sql.append(")");
                sql.append(" AND (EXISTS (SELECT 1 FROM category_attribute ca WHERE ca.category_id = p.category_id")
                        .append(" AND ca.attribute_id = pav.attribute_id AND ca.is_filterable = 1)")
                        .append(" OR (#{query.conditions[").append(i).append("].fallbackFilterable} = TRUE")
                        .append(" AND NOT EXISTS (SELECT 1 FROM category_attribute override_ca")
                        .append(" WHERE override_ca.category_id = p.category_id")
                        .append(" AND override_ca.attribute_id = pav.attribute_id)))");
                sql.append(")");
            }
            return sql.toString();
        }

        private static String baseVisibility(String englishParameter) {
            return " WHERE p.status = 'PUBLISHED' AND p.deleted_at IS NULL"
                    + " AND c.status = 'ACTIVE' AND c.deleted_at IS NULL"
                    + " AND (" + englishParameter + " = FALSE OR p.english_status = 'CONFIRMED')";
        }
    }
}
