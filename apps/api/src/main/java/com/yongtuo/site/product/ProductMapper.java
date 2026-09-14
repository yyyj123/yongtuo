package com.yongtuo.site.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM product WHERE id = #{id} AND deleted_at IS NULL FOR UPDATE")
    Product selectLiveByIdForUpdate(long id);
}
