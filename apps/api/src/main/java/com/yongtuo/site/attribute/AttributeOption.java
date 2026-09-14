package com.yongtuo.site.attribute;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("attribute_option")
public class AttributeOption {
    @TableId(type = IdType.AUTO) private Long id;
    private Long attributeId;
    private String valueCode;
    private String labelZh;
    private String labelEn;
    private Integer sortOrder;
    private AttributeStatus status;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getAttributeId() { return attributeId; }
    public void setAttributeId(Long value) { attributeId = value; }
    public String getValueCode() { return valueCode; }
    public void setValueCode(String value) { valueCode = value; }
    public String getLabelZh() { return labelZh; }
    public void setLabelZh(String value) { labelZh = value; }
    public String getLabelEn() { return labelEn; }
    public void setLabelEn(String value) { labelEn = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
    public AttributeStatus getStatus() { return status; }
    public void setStatus(AttributeStatus value) { status = value; }
}
