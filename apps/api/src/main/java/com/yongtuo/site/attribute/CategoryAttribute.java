package com.yongtuo.site.attribute;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("category_attribute")
public class CategoryAttribute {
    @TableId(type = IdType.AUTO) private Long id;
    private Long categoryId;
    private Long attributeId;
    private Boolean isFilterable;
    private Boolean isRequired;
    private Boolean showInDetail;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long value) { categoryId = value; }
    public Long getAttributeId() { return attributeId; }
    public void setAttributeId(Long value) { attributeId = value; }
    public Boolean getIsFilterable() { return isFilterable; }
    public void setIsFilterable(Boolean value) { isFilterable = value; }
    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean value) { isRequired = value; }
    public Boolean getShowInDetail() { return showInDetail; }
    public void setShowInDetail(Boolean value) { showInDetail = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
}
