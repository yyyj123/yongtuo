package com.yongtuo.site.search;

import com.yongtuo.site.product.Product;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SearchMapper {
    String FROM_AND_WHERE = """
            FROM product p
            JOIN product_category c ON c.id = p.category_id
            WHERE p.status = 'PUBLISHED'
              AND p.deleted_at IS NULL
              AND c.status = 'ACTIVE'
              AND c.deleted_at IS NULL
              AND (#{english} = FALSE OR p.english_status = 'CONFIRMED')
              AND (
                    LOCATE(#{keyword}, COALESCE(p.name_zh, '')) > 0
                 OR LOCATE(#{keyword}, COALESCE(p.name_en, '')) > 0
                 OR LOCATE(#{keyword}, p.product_code) > 0
                 OR LOCATE(#{keyword}, COALESCE(c.name_zh, '')) > 0
                 OR LOCATE(#{keyword}, COALESCE(c.name_en, '')) > 0
                 OR EXISTS (
                      SELECT 1 FROM product_variant pv
                      WHERE pv.product_id = p.id
                        AND pv.status = 'PUBLISHED'
                        AND LOCATE(#{keyword}, pv.variant_code) > 0
                 )
                 OR EXISTS (
                      SELECT 1
                      FROM product_attribute_value pav
                      JOIN attribute_definition ad
                        ON ad.id = pav.attribute_id
                       AND ad.status = 'ACTIVE'
                       AND ad.code IN ('material', 'standard')
                      JOIN category_attribute ca
                        ON ca.category_id = p.category_id
                       AND ca.attribute_id = pav.attribute_id
                       AND ca.show_in_detail = TRUE
                      LEFT JOIN attribute_option ao
                        ON ao.id = pav.option_id
                       AND ao.attribute_id = pav.attribute_id
                       AND ao.status = 'ACTIVE'
                      WHERE pav.product_id = p.id
                        AND (
                              LOCATE(#{keyword}, COALESCE(pav.value_zh, '')) > 0
                           OR LOCATE(#{keyword}, COALESCE(pav.value_en, '')) > 0
                           OR LOCATE(#{keyword}, COALESCE(ao.label_zh, '')) > 0
                           OR LOCATE(#{keyword}, COALESCE(ao.label_en, '')) > 0
                        )
                 )
                 OR EXISTS (
                      SELECT 1
                      FROM product_variant pv
                      JOIN product_variant_value pvv ON pvv.variant_id = pv.id
                      JOIN attribute_definition ad
                        ON ad.id = pvv.attribute_id
                       AND ad.status = 'ACTIVE'
                       AND ad.code IN ('material', 'standard')
                      JOIN category_attribute ca
                        ON ca.category_id = p.category_id
                       AND ca.attribute_id = pvv.attribute_id
                       AND ca.show_in_detail = TRUE
                      LEFT JOIN attribute_option ao
                        ON ao.id = pvv.option_id
                       AND ao.attribute_id = pvv.attribute_id
                       AND ao.status = 'ACTIVE'
                      WHERE pv.product_id = p.id
                        AND pv.status = 'PUBLISHED'
                        AND (
                              LOCATE(#{keyword}, COALESCE(pvv.value_zh, '')) > 0
                           OR LOCATE(#{keyword}, COALESCE(pvv.value_en, '')) > 0
                           OR LOCATE(#{keyword}, COALESCE(ao.label_zh, '')) > 0
                           OR LOCATE(#{keyword}, COALESCE(ao.label_en, '')) > 0
                        )
                 )
              )
            """;

    @Select("SELECT p.* " + FROM_AND_WHERE
            + " ORDER BY p.sort_order ASC, p.id ASC LIMIT #{limit}")
    List<Product> find(@Param("keyword") String keyword, @Param("english") boolean english,
                       @Param("limit") int limit);

    @Select("SELECT COUNT(*) " + FROM_AND_WHERE)
    long count(@Param("keyword") String keyword, @Param("english") boolean english);
}
