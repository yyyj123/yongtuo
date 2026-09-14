package com.yongtuo.site.attribute;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("product_attribute_value")
public class ProductAttributeValue {
    @TableId(type = IdType.AUTO) private Long id;
    private Long productId;
    private Long attributeId;
    private String valueZh;
    private String valueEn;
    private BigDecimal numericValue;
    private Long optionId;
    private Long valueKey;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId = value; }
    public Long getAttributeId() { return attributeId; }
    public void setAttributeId(Long value) { attributeId = value; }
    public String getValueZh() { return valueZh; }
    public void setValueZh(String value) { valueZh = value; }
    public String getValueEn() { return valueEn; }
    public void setValueEn(String value) { valueEn = value; }
    public BigDecimal getNumericValue() { return numericValue; }
    public void setNumericValue(BigDecimal value) { numericValue = value; }
    public Long getOptionId() { return optionId; }
    public void setOptionId(Long value) { optionId = value; }
    public Long getValueKey() { return valueKey; }
    public void setValueKey(Long value) { valueKey = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
}
