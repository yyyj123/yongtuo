package com.yongtuo.site.product.variant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("product_variant_value")
public class ProductVariantValue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long variantId;
    private Long attributeId;
    private Long optionId;
    private Long valueKey;
    private String valueZh;
    private String valueEn;
    private BigDecimal numericValue;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getVariantId() { return variantId; }
    public void setVariantId(Long value) { variantId = value; }
    public Long getAttributeId() { return attributeId; }
    public void setAttributeId(Long value) { attributeId = value; }
    public Long getOptionId() { return optionId; }
    public void setOptionId(Long value) { optionId = value; }
    public Long getValueKey() { return valueKey; }
    public void setValueKey(Long value) { valueKey = value; }
    public String getValueZh() { return valueZh; }
    public void setValueZh(String value) { valueZh = value; }
    public String getValueEn() { return valueEn; }
    public void setValueEn(String value) { valueEn = value; }
    public BigDecimal getNumericValue() { return numericValue; }
    public void setNumericValue(BigDecimal value) { numericValue = value; }
}
