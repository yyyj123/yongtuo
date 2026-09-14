package com.yongtuo.site.attribute;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("attribute_definition")
public class AttributeDefinition {
    @TableId(type = IdType.AUTO) private Long id;
    private String nameZh;
    private String nameEn;
    private String code;
    private AttributeDataType dataType;
    private String unit;
    private Boolean isGlobal;
    private Boolean defaultFilterable;
    private Boolean defaultRequired;
    private Integer sortOrder;
    private AttributeStatus status;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String value) { nameZh = value; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String value) { nameEn = value; }
    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public AttributeDataType getDataType() { return dataType; }
    public void setDataType(AttributeDataType value) { dataType = value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }
    public Boolean getIsGlobal() { return isGlobal; }
    public void setIsGlobal(Boolean value) { isGlobal = value; }
    public Boolean getDefaultFilterable() { return defaultFilterable; }
    public void setDefaultFilterable(Boolean value) { defaultFilterable = value; }
    public Boolean getDefaultRequired() { return defaultRequired; }
    public void setDefaultRequired(Boolean value) { defaultRequired = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
    public AttributeStatus getStatus() { return status; }
    public void setStatus(AttributeStatus value) { status = value; }
}
