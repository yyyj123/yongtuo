package com.yongtuo.site.category;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper extends BaseMapper<ProductCategory> {

    @Select("""
            SELECT id, parent_id, name_zh, name_en, slug, cover_image,
                   description_zh, description_en, category_mode, sort_order, status,
                   show_on_home, seo_title_zh, seo_title_en, seo_description_zh,
                   seo_description_en, created_at, updated_at, deleted_at
            FROM product_category
            WHERE deleted_at IS NULL
            ORDER BY id
            FOR UPDATE
            """)
    List<ProductCategory> selectLiveHierarchyForUpdate();

    @Select("SELECT id, parent_id, name_zh, name_en, slug, category_mode, sort_order, status, show_on_home "
            + "FROM product_category WHERE id = #{id} AND deleted_at IS NULL FOR UPDATE")
    ProductCategory selectLiveByIdForUpdate(long id);
}
